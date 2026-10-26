package com.jinrui.xlxadmin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinrui.xlxadmin.dto.ClientDtos.SupportReportMeta;
import com.jinrui.xlxadmin.dto.ClientDtos.SupportReportResult;
import com.jinrui.xlxadmin.entity.ClientEvent;
import com.jinrui.xlxadmin.entity.DeviceRecord;
import com.jinrui.xlxadmin.entity.LicenseRecord;
import com.jinrui.xlxadmin.entity.SupportReport;
import com.jinrui.xlxadmin.mapper.SupportReportMapper;
import com.jinrui.xlxadmin.service.DeviceService;
import com.jinrui.xlxadmin.service.EventService;
import com.jinrui.xlxadmin.service.LicenseService;
import com.jinrui.xlxadmin.service.SupportReportService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 售后反馈（一键提交日志）—— 客户端在启动修复/重置失败时上传诊断日志包。
 *
 * 与常规客户端接口不同，这里对授权状态做宽松处理：重置失败往往伴随授权校验异常，
 * 最需要日志时不能因为授权问题拒收。代之以三层兜底：
 *   1) 客户端内置 secret 的 HMAC 签名（xlx.report-sign-required 开启后强制）
 *   2) 单设备每日 / 单 IP 每分钟频率限流（内存计数，重启即清零）
 *   3) reportNo 唯一键保证客户端断网重试的幂等（重试直接返回已有回执）
 *
 * 提交成功后同步落一条 client_event（eventType=support_report_submitted），
 * 让现有日志时间线与仪表盘统计不需要额外改造。
 */
@Service
public class SupportReportServiceImpl extends ServiceImpl<SupportReportMapper, SupportReport>
    implements SupportReportService {
  private static final Set<String> SCENE_WHITELIST = Set.of(
      "repair_failed", "reset_failed", "startup_failed", "update_failed", "manual");
  private static final Set<String> STATUS_WHITELIST = Set.of("submitted", "triaged", "resolved", "ignored");
  private static final String EVENT_TYPE = "support_report_submitted";
  private static final int MAX_PER_DEVICE_PER_DAY = 5;
  private static final int MAX_PER_IP_PER_MINUTE = 10;
  private static final long DEVICE_WINDOW_MILLIS = Duration.ofDays(1).toMillis();
  private static final long IP_WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();
  private static final long SIGN_WINDOW_MILLIS = Duration.ofMinutes(10).toMillis();
  private static final int RATE_MAP_CLEANUP_THRESHOLD = 4096;
  /** 过期清理每批删除条数：避免一次性大事务，也防止长事务拖住上传接口。 */
  private static final int PURGE_BATCH_SIZE = 200;
  private static final DateTimeFormatter REPORT_NO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final ReportFileStore reportFileStore;
  private final LicenseService licenseService;
  private final DeviceService deviceService;
  private final EventService eventService;
  private final ObjectMapper objectMapper;
  private final long maxBytes;
  private final String clientSecret;
  private final boolean signRequired;

  /** 限流窗口计数，key 为设备标识 / IP，仅内存态（重启清零，不跨实例）。 */
  private final Map<String, RateWindow> deviceWindows = new ConcurrentHashMap<>();
  private final Map<String, RateWindow> ipWindows = new ConcurrentHashMap<>();

  public SupportReportServiceImpl(ReportFileStore reportFileStore,
                                  LicenseService licenseService,
                                  DeviceService deviceService,
                                  EventService eventService,
                                  ObjectMapper objectMapper,
                                  @Value("${xlx.report-max-bytes:52428800}") long maxBytes,
                                  @Value("${xlx.client-secret:change-me-client-report-secret}") String clientSecret,
                                  @Value("${xlx.report-sign-required:false}") boolean signRequired) {
    this.reportFileStore = reportFileStore;
    this.licenseService = licenseService;
    this.deviceService = deviceService;
    this.eventService = eventService;
    this.objectMapper = objectMapper;
    this.maxBytes = maxBytes;
    this.clientSecret = clientSecret;
    this.signRequired = signRequired;
  }

  @Override
  @Transactional
  public SupportReportResult submit(SupportReportMeta meta, MultipartFile file, HttpServletRequest http) {
    SupportReportMeta safeMeta = meta == null ? emptyMeta() : meta;
    String clientReportNo = cleanReportNo(safeMeta.reportNo());
    String ip = clientIp(http);

    verifySignature(clientReportNo, http);

    String reportNo = clientReportNo.isBlank() ? generateReportNo() : clientReportNo;
    SupportReport existing = getOne(Wrappers.lambdaQuery(SupportReport.class)
        .eq(SupportReport::getReportNo, reportNo)
        .last("limit 1"));
    if (existing != null) {
      return toResult(existing, true);
    }

    checkRateLimit(safeMeta, ip);

    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("缺少日志包文件：multipart 字段 file 不能为空");
    }
    if (file.getSize() > maxBytes) {
      throw new IllegalArgumentException("日志包超过大小上限（" + (maxBytes / 1024 / 1024) + " MB）");
    }
    assertZipMagic(file);

    LicenseRecord license = findLicense(safeMeta.licenseKey());
    DeviceRecord device = license == null ? null : findDevice(license.getId(), safeMeta.machineId());

    ReportFileStore.StoredFile stored;
    try (InputStream in = file.getInputStream()) {
      stored = reportFileStore.save(in, reportNo);
    } catch (IOException e) {
      throw new IllegalArgumentException("日志包保存失败：" + e.getMessage());
    }

    SupportReport report = new SupportReport();
    report.setReportNo(reportNo);
    if (license != null) report.setLicenseId(license.getId());
    if (device != null) report.setDeviceId(device.getId());
    report.setLicenseKey(trimTo(safeMeta.licenseKey(), 64));
    report.setUsbId(trimTo(safeMeta.usbId(), 128));
    report.setMachineId(trimTo(safeMeta.machineId(), 128));
    report.setHostname(trimTo(safeMeta.hostname(), 128));
    report.setOsName(trimTo(safeMeta.os(), 128));
    report.setArch(trimTo(safeMeta.arch(), 32));
    report.setClientVersion(trimTo(safeMeta.clientVersion(), 64));
    report.setScene(normalizeScene(safeMeta.scene()));
    report.setErrorCode(trimTo(safeMeta.errorCode(), 64));
    report.setErrorSummary(trimTo(safeMeta.errorSummary(), 1024));
    report.setRemark(trimTo(safeMeta.remark(), 512));
    report.setContact(trimTo(safeMeta.contact(), 128));
    report.setFilePath(stored.relativePath());
    report.setFileSize(stored.size());
    report.setFileSha256(stored.sha256());
    report.setSourceIp(ip);
    report.setStatus("submitted");
    try {
      save(report);
    } catch (RuntimeException e) {
      // 落库失败（含并发重复回执码触发的唯一键冲突）：补偿删除文件，由客户端重试命中幂等分支
      reportFileStore.deleteQuietly(stored.relativePath());
      throw e;
    }
    recordTimelineEvent(report);
    return toResult(report, false);
  }

  @Override
  public Path filePath(SupportReport report) {
    return report == null ? null : reportFileStore.resolve(report.getFilePath());
  }

  @Override
  public SupportReport updateStatus(Long id, String status, String adminNote) {
    SupportReport report = getById(id);
    if (report == null) {
      throw new IllegalArgumentException("反馈记录不存在：" + id);
    }
    if (!isBlank(status)) {
      String cleaned = status.trim().toLowerCase(Locale.ROOT);
      if (!STATUS_WHITELIST.contains(cleaned)) {
        throw new IllegalArgumentException("非法状态：" + status + "，可选 " + STATUS_WHITELIST);
      }
      report.setStatus(cleaned);
    }
    if (adminNote != null) {
      report.setAdminNote(trimTo(adminNote, 1024));
    }
    updateById(report);
    return report;
  }

  /**
   * 过期清理：按 id 升序分批取待删记录，先删库再删文件（库是权威数据，文件删除失败只影响磁盘）。
   * 不加 @Transactional，让每批独立提交，避免积压日志时形成长事务。
   */
  @Override
  public int purgeExpired(LocalDateTime before) {
    if (before == null) return 0;
    int deleted = 0;
    while (true) {
      List<SupportReport> batch = list(Wrappers.lambdaQuery(SupportReport.class)
          .select(SupportReport::getId, SupportReport::getFilePath)
          .lt(SupportReport::getCreatedAt, before)
          .orderByAsc(SupportReport::getId)
          .last("limit " + PURGE_BATCH_SIZE));
      if (batch.isEmpty()) break;
      removeByIds(batch.stream().map(SupportReport::getId).toList());
      batch.forEach(report -> reportFileStore.deleteQuietly(report.getFilePath()));
      deleted += batch.size();
      if (batch.size() < PURGE_BATCH_SIZE) break;
    }
    return deleted;
  }

  /**
   * 签名校验：X-Client-Ts（毫秒时间戳）+ X-Client-Sign = HMAC-SHA256(ts + "\n" + reportNo)。
   * 未开启 xlx.report-sign-required 时跳过（灰度期客户端尚未接入签名）。
   */
  private void verifySignature(String reportNo, HttpServletRequest http) {
    if (!signRequired) return;
    String ts = header(http, "X-Client-Ts");
    String sign = header(http, "X-Client-Sign");
    if (ts.isBlank() || sign.isBlank()) {
      throw new IllegalArgumentException("缺少客户端签名（X-Client-Ts / X-Client-Sign）");
    }
    if (reportNo.isBlank()) {
      throw new IllegalArgumentException("签名校验开启时必须携带 reportNo");
    }
    long tsMillis;
    try {
      tsMillis = Long.parseLong(ts);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("X-Client-Ts 不是合法毫秒时间戳");
    }
    if (Math.abs(System.currentTimeMillis() - tsMillis) > SIGN_WINDOW_MILLIS) {
      throw new IllegalArgumentException("客户端时间戳超出允许范围（±10 分钟）");
    }
    String expected = hmacHex(ts + "\n" + reportNo);
    if (!expected.equalsIgnoreCase(sign)) {
      throw new IllegalArgumentException("客户端签名校验失败");
    }
  }

  private void checkRateLimit(SupportReportMeta meta, String ip) {
    long now = System.currentTimeMillis();
    RateWindow deviceWindow = deviceWindows.computeIfAbsent(deviceKey(meta, ip), key -> new RateWindow());
    if (!deviceWindow.tryAcquire(now, DEVICE_WINDOW_MILLIS, MAX_PER_DEVICE_PER_DAY)) {
      throw new IllegalArgumentException("该设备今日提交次数已达上限（" + MAX_PER_DEVICE_PER_DAY + " 次），请明日再试或联系技术支持");
    }
    RateWindow ipWindow = ipWindows.computeIfAbsent(ip, key -> new RateWindow());
    if (!ipWindow.tryAcquire(now, IP_WINDOW_MILLIS, MAX_PER_IP_PER_MINUTE)) {
      throw new IllegalArgumentException("提交过于频繁，请稍后再试");
    }
    cleanupRateMaps(now);
  }

  /** 超阈值时惰性清理过期窗口，防止长期运行内存膨胀。 */
  private void cleanupRateMaps(long now) {
    if (deviceWindows.size() > RATE_MAP_CLEANUP_THRESHOLD) {
      deviceWindows.entrySet().removeIf(entry -> entry.getValue().expired(now, DEVICE_WINDOW_MILLIS));
    }
    if (ipWindows.size() > RATE_MAP_CLEANUP_THRESHOLD) {
      ipWindows.entrySet().removeIf(entry -> entry.getValue().expired(now, IP_WINDOW_MILLIS));
    }
  }

  private void recordTimelineEvent(SupportReport report) {
    ClientEvent event = new ClientEvent();
    event.setLicenseId(report.getLicenseId());
    event.setDeviceId(report.getDeviceId());
    event.setLicenseKey(report.getLicenseKey());
    event.setEventType(EVENT_TYPE);
    event.setLevel("warn");
    String summary = report.getErrorSummary() == null ? "" : report.getErrorSummary();
    event.setMessage(trimTo(report.getScene() + "：" + summary, 1024));
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("reportNo", report.getReportNo());
    details.put("scene", report.getScene());
    if (report.getClientVersion() != null) details.put("clientVersion", report.getClientVersion());
    details.put("fileSize", report.getFileSize());
    if (report.getFileSha256() != null) details.put("sha256", report.getFileSha256());
    try {
      event.setDetailsJson(objectMapper.writeValueAsString(details));
    } catch (Exception e) {
      event.setDetailsJson("{}");
    }
    eventService.save(event);
  }

  private LicenseRecord findLicense(String licenseKey) {
    if (isBlank(licenseKey)) return null;
    return licenseService.getOne(Wrappers.lambdaQuery(LicenseRecord.class)
        .eq(LicenseRecord::getLicenseKey, licenseKey.trim())
        .last("limit 1"));
  }

  private DeviceRecord findDevice(Long licenseId, String machineId) {
    var wrapper = Wrappers.lambdaQuery(DeviceRecord.class)
        .eq(DeviceRecord::getLicenseId, licenseId);
    if (!isBlank(machineId)) {
      wrapper.eq(DeviceRecord::getMachineId, machineId.trim());
    }
    wrapper.orderByDesc(DeviceRecord::getLastSeenAt).last("limit 1");
    return deviceService.getOne(wrapper);
  }

  /** 日志包统一要求 zip 格式（PK 文件头），便于后台拿到后直接解压诊断。 */
  private static void assertZipMagic(MultipartFile file) {
    try (InputStream in = file.getInputStream()) {
      byte[] head = in.readNBytes(4);
      boolean zip = head.length >= 4 && head[0] == 'P' && head[1] == 'K'
          && ((head[2] == 3 && head[3] == 4) || (head[2] == 5 && head[3] == 6) || (head[2] == 7 && head[3] == 8));
      if (!zip) {
        throw new IllegalArgumentException("日志包必须是 zip 格式（缺少 PK 文件头）");
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("读取日志包失败：" + e.getMessage());
    }
  }

  private static SupportReportMeta emptyMeta() {
    return new SupportReportMeta(null, null, null, null, null, null, null, null, null, null, null, null, null);
  }

  private static SupportReportResult toResult(SupportReport report, boolean duplicate) {
    LocalDateTime receivedAt = report.getCreatedAt() == null ? LocalDateTime.now() : report.getCreatedAt();
    return new SupportReportResult(report.getReportNo(), report.getStatus(), receivedAt, duplicate,
        "日志已提交，回执码 " + report.getReportNo() + "。客服会尽快跟进，如需人工支持请提供该回执码。");
  }

  /** 过滤客户端自定义回执码中的非法字符；长度不足 8 视为未提供，由服务端兜底生成。 */
  private static String cleanReportNo(String raw) {
    if (raw == null) return "";
    String cleaned = raw.trim().replaceAll("[^A-Za-z0-9-]", "");
    if (cleaned.length() > 64) cleaned = cleaned.substring(0, 64);
    return cleaned.length() < 8 ? "" : cleaned;
  }

  private static String generateReportNo() {
    String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    return "RPT-" + REPORT_NO_TIME_FORMAT.format(LocalDateTime.now()) + "-" + random;
  }

  private static String normalizeScene(String scene) {
    String cleaned = scene == null ? "" : scene.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
    if (cleaned.isBlank()) return "manual";
    return SCENE_WHITELIST.contains(cleaned) ? cleaned : "other";
  }

  private static String deviceKey(SupportReportMeta meta, String ip) {
    if (!isBlank(meta.machineId())) return "m:" + meta.machineId().trim();
    String licenseKey = isBlank(meta.licenseKey()) ? "" : meta.licenseKey().trim();
    String usbId = isBlank(meta.usbId()) ? "" : meta.usbId().trim();
    if (!licenseKey.isEmpty() || !usbId.isEmpty()) return "l:" + licenseKey + "|" + usbId;
    return "ip:" + ip;
  }

  private String hmacHex(String payload) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
      }
      return sb.toString();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HMAC-SHA256 not available", e);
    }
  }

  private static String trimTo(String value, int maxLength) {
    if (value == null) return null;
    String trimmed = value.trim();
    if (trimmed.isEmpty()) return null;
    return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
  }

  private static String header(HttpServletRequest request, String name) {
    if (request == null) return "";
    String value = request.getHeader(name);
    return value == null ? "" : value.trim();
  }

  private static String clientIp(HttpServletRequest request) {
    if (request == null) return "";
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
    return request.getRemoteAddr();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  /** 固定窗口计数器：窗口过期时重置，命中上限返回 false。 */
  private static final class RateWindow {
    private long windowStart;
    private int count;

    synchronized boolean tryAcquire(long now, long windowMillis, int limit) {
      if (now - windowStart >= windowMillis) {
        windowStart = now;
        count = 0;
      }
      if (count >= limit) return false;
      count++;
      return true;
    }

    synchronized boolean expired(long now, long windowMillis) {
      return now - windowStart >= windowMillis;
    }
  }
}