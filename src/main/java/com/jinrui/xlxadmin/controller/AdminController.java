package com.jinrui.xlxadmin.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinrui.xlxadmin.dto.AdminDtos.LicenseSaveRequest;
import com.jinrui.xlxadmin.dto.AdminDtos.ModelProviderSaveRequest;
import com.jinrui.xlxadmin.dto.AdminDtos.SupportReportUpdateRequest;
import com.jinrui.xlxadmin.dto.AdminDtos.VersionSaveRequest;
import com.jinrui.xlxadmin.dto.ApiResponse;
import com.jinrui.xlxadmin.entity.ClientEvent;
import com.jinrui.xlxadmin.entity.LicenseRecord;
import com.jinrui.xlxadmin.entity.ModelProvider;
import com.jinrui.xlxadmin.entity.SupportReport;
import com.jinrui.xlxadmin.entity.VersionInfo;
import com.jinrui.xlxadmin.service.DeviceService;
import com.jinrui.xlxadmin.service.EventService;
import com.jinrui.xlxadmin.service.LicenseService;
import com.jinrui.xlxadmin.service.ModelConfigService;
import com.jinrui.xlxadmin.service.SupportReportService;
import com.jinrui.xlxadmin.service.VersionService;
import com.jinrui.xlxadmin.service.impl.VersionPackageService;
import com.jinrui.xlxadmin.util.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 后台管理接口 —— 授权、设备、版本、模型、事件的后台 CRUD。
 * <p>
 * 鉴权方式：Authorization: Bearer &lt;jwt&gt;。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final JwtService jwtService;
  private final LicenseService licenseService;
  private final DeviceService deviceService;
  private final VersionService versionService;
  private final VersionPackageService versionPackageService;
  private final ModelConfigService modelConfigService;
  private final EventService eventService;
  private final SupportReportService supportReportService;

  public AdminController(JwtService jwtService,
                         LicenseService licenseService, DeviceService deviceService,
                         VersionService versionService, VersionPackageService versionPackageService,
                         ModelConfigService modelConfigService,
                         EventService eventService,
                         SupportReportService supportReportService) {
    this.jwtService = jwtService;
    this.licenseService = licenseService;
    this.deviceService = deviceService;
    this.versionService = versionService;
    this.versionPackageService = versionPackageService;
    this.modelConfigService = modelConfigService;
    this.eventService = eventService;
    this.supportReportService = supportReportService;
  }

  /**
   * 仪表盘概览 —— 返回各模块的数量汇总、近24小时错误数、今日网关启动统计。
   */
  @GetMapping("/summary")
  public ApiResponse<?> summary(@RequestHeader(value = "Authorization", required = false) String authorization) {
    check(authorization);
    licenseService.expireOverdueLicenses();
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("licenses", licenseService.count());
    map.put("activeLicenses", licenseService.count(Wrappers.lambdaQuery(LicenseRecord.class)
        .eq(LicenseRecord::getStatus, "active")));
    map.put("devices", deviceService.count());
    map.put("versions", versionService.count());
    map.put("models", modelConfigService.count());
    map.put("events", eventService.count());
    map.put("errors24h", eventService.count(Wrappers.lambdaQuery(ClientEvent.class)
        .eq(ClientEvent::getLevel, "error")
        .gt(ClientEvent::getCreatedAt, LocalDateTime.now().minusDays(1))));
    map.put("reports", supportReportService.count());
    map.put("reports24h", supportReportService.count(Wrappers.lambdaQuery(SupportReport.class)
        .gt(SupportReport::getCreatedAt, LocalDateTime.now().minusDays(1))));
    map.put("reportsPending", supportReportService.count(Wrappers.lambdaQuery(SupportReport.class)
        .eq(SupportReport::getStatus, "submitted")));
    LocalDateTime todayStart = LocalDate.now().atStartOfDay();
    long gatewaySuccessToday = eventService.count(Wrappers.lambdaQuery(ClientEvent.class)
        .eq(ClientEvent::getEventType, "gateway_start_success")
        .ge(ClientEvent::getCreatedAt, todayStart));
    long gatewayFailedToday = eventService.count(Wrappers.lambdaQuery(ClientEvent.class)
        .eq(ClientEvent::getEventType, "gateway_start_failed")
        .ge(ClientEvent::getCreatedAt, todayStart));
    long gatewayStartsToday = gatewaySuccessToday + gatewayFailedToday;
    map.put("gatewayStartsToday", gatewayStartsToday);
    map.put("gatewaySuccessToday", gatewaySuccessToday);
    map.put("gatewayFailedToday", gatewayFailedToday);
    map.put("gatewaySuccessRateToday", gatewayStartsToday == 0
        ? "0%"
        : String.format("%.1f%%", gatewaySuccessToday * 100.0 / gatewayStartsToday));
    return ApiResponse.ok(map);
  }

  /**
   * 授权列表 —— 支持分页和按字段模糊查询。
   */
  @GetMapping("/licenses")
  public ApiResponse<?> licenses(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "20") int pageSize,
                                 @RequestParam(defaultValue = "") String licenseKey,
                                 @RequestParam(defaultValue = "") String customerName,
                                 @RequestParam(defaultValue = "") String status,
                                 @RequestParam(defaultValue = "") String usbId) {
    check(authorization);
    licenseService.expireOverdueLicenses();
    var wrapper = Wrappers.lambdaQuery(LicenseRecord.class).orderByDesc(LicenseRecord::getId);
    if (!licenseKey.isBlank()) wrapper.like(LicenseRecord::getLicenseKey, licenseKey);
    if (!customerName.isBlank()) wrapper.like(LicenseRecord::getCustomerName, customerName);
    if (!status.isBlank()) wrapper.like(LicenseRecord::getStatus, status);
    if (!usbId.isBlank()) wrapper.like(LicenseRecord::getUsbId, usbId);
    return ApiResponse.ok(licenseService.page(new Page<>(page, pageSize), wrapper));
  }

  /**
   * 创建授权 —— 自动生成 licenseKey，状态默认 active。
   */
  @PostMapping("/licenses")
  public ApiResponse<?> createLicense(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestBody LicenseSaveRequest req) {
    check(authorization);
    return ApiResponse.ok(licenseService.save(null, req));
  }

  /**
   * 编辑授权 —— 修改客户名称、状态、过期时间、通道等字段。
   */
  @PatchMapping("/licenses/{id}")
  public ApiResponse<?> updateLicense(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable Long id, @RequestBody LicenseSaveRequest req) {
    check(authorization);
    return ApiResponse.ok(licenseService.save(id, req));
  }

  /**
   * 删除授权 —— 删除后该授权码不能再通过客户端校验。
   */
  @DeleteMapping("/licenses/{id}")
  public ApiResponse<?> deleteLicense(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable Long id) {
    check(authorization);
    boolean deleted = licenseService.removeById(id);
    return ApiResponse.ok(Map.of("deleted", deleted, "id", id));
  }

  /**
   * 设备列表 —— 按 license_id + usb_id + hostname 分组聚合展示，附带授权码和累计启动次数。
   */
  @GetMapping("/devices")
  public ApiResponse<?> devices(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int pageSize,
                                @RequestParam(defaultValue = "") String licenseKey,
                                @RequestParam(defaultValue = "") String hostname,
                                @RequestParam(defaultValue = "") String usbId,
                                @RequestParam(defaultValue = "") String lastIp) {
    check(authorization);
    int safePage = Math.max(page, 1);
    int safePageSize = Math.min(Math.max(pageSize, 1), 100);
    return ApiResponse.ok(deviceService.selectGroupedDevices(new Page<>(safePage, safePageSize),
        licenseKey.trim(), hostname.trim(), usbId.trim(), lastIp.trim()));
  }

  /**
   * 版本列表 —— 按创建时间倒序，最多返回 50 条。
   */
  @GetMapping("/versions")
  public ApiResponse<?> versions(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int pageSize,
                                @RequestParam(defaultValue = "") String keyword,
                                @RequestParam(defaultValue = "") String platform,
                                @RequestParam(defaultValue = "") String arch,
                                @RequestParam(defaultValue = "") String published) {
    check(authorization);
    int safePage = Math.max(page, 1);
    int safePageSize = Math.min(Math.max(pageSize, 1), 100);
    var wrapper = Wrappers.lambdaQuery(VersionInfo.class).orderByDesc(VersionInfo::getCreatedAt);
    String safeKeyword = keyword.trim();
    if (!safeKeyword.isBlank()) {
      wrapper.and(item -> item.like(VersionInfo::getVersion, safeKeyword)
          .or().like(VersionInfo::getTitle, safeKeyword)
          .or().like(VersionInfo::getReleaseNotes, safeKeyword)
          .or().like(VersionInfo::getChannel, safeKeyword));
    }
    if (!platform.isBlank()) wrapper.like(VersionInfo::getPlatform, platform.trim());
    if (!arch.isBlank()) wrapper.like(VersionInfo::getArch, arch.trim());
    if ("true".equalsIgnoreCase(published)) wrapper.eq(VersionInfo::isPublished, true);
    if ("false".equalsIgnoreCase(published)) wrapper.eq(VersionInfo::isPublished, false);
    return ApiResponse.ok(versionService.page(new Page<>(safePage, safePageSize), wrapper));
  }

  /**
   * 创建版本 —— 上传新版本信息（版本号、下载地址、更新日志等），默认未发布。
   */
  @PostMapping("/versions")
  public ApiResponse<?> createVersion(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestBody VersionSaveRequest req) {
    check(authorization);
    return ApiResponse.ok(versionService.save(null, req));
  }

  /**
   * 上传版本安装包 —— 将 exe/zip 文件上传到服务器，返回下载地址。
   */
  @PostMapping("/versions/upload")
  public ApiResponse<?> uploadVersionPackage(@RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestParam("file") MultipartFile file,
                                             HttpServletRequest request) {
    check(authorization);
    return ApiResponse.ok("上传成功", versionPackageService.upload(file, request));
  }

  /**
   * 编辑版本 —— 修改版本信息或切换发布状态。
   */
  @PatchMapping("/versions/{id}")
  public ApiResponse<?> updateVersion(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable Long id, @RequestBody VersionSaveRequest req) {
    check(authorization);
    return ApiResponse.ok(versionService.save(id, req));
  }

  /**
   * 删除版本。
   */
  @DeleteMapping("/versions/{id}")
  public ApiResponse<?> deleteVersion(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable Long id) {
    check(authorization);
    versionService.removeById(id);
    return ApiResponse.ok(Map.of("deleted", true, "id", id));
  }

  /**
   * 模型列表 —— 按 ID 倒序返回全部模型供应商配置。
   */
  @GetMapping("/models")
  public ApiResponse<?> models(@RequestHeader(value = "Authorization", required = false) String authorization) {
    check(authorization);
    return ApiResponse.ok(modelConfigService.list(Wrappers.lambdaQuery(ModelProvider.class).orderByDesc(ModelProvider::getId)));
  }

  /**
   * 创建模型 —— 新增一个模型供应商（providerId 不可重复），keyMode 默认 client。
   */
  @PostMapping("/models")
  public ApiResponse<?> createModel(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestBody ModelProviderSaveRequest req) {
    check(authorization);
    return ApiResponse.ok(modelConfigService.save(null, req));
  }

  /**
   * 编辑模型 —— 修改模型配置，保存时 revision 自动 +1。
   */
  @PatchMapping("/models/{id}")
  public ApiResponse<?> updateModel(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable Long id, @RequestBody ModelProviderSaveRequest req) {
    check(authorization);
    return ApiResponse.ok(modelConfigService.save(id, req));
  }

  /**
   * 删除模型。
   */
  @DeleteMapping("/models/{id}")
  public ApiResponse<?> deleteModel(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable Long id) {
    check(authorization);
    modelConfigService.removeById(id);
    return ApiResponse.ok(Map.of("deleted", true, "id", id));
  }

  /**
   * 事件列表 —— 分页返回，按创建时间倒序，每页最多 100 条。
   */
  @GetMapping("/events")
  public ApiResponse<?> events(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int pageSize) {
    check(authorization);
    int safePage = Math.max(page, 1);
    int safePageSize = Math.min(Math.max(pageSize, 1), 100);
    Page<ClientEvent> result = eventService.page(new Page<>(safePage, safePageSize),
        Wrappers.lambdaQuery(ClientEvent.class).orderByDesc(ClientEvent::getCreatedAt));
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("records", result.getRecords());
    body.put("current", result.getCurrent());
    body.put("pages", result.getPages());
    body.put("total", result.getTotal());
    body.put("size", result.getSize());
    return ApiResponse.ok(body);
  }

  /**
   * 反馈列表 —— 售后「一键提交日志」记录，支持场景/状态/授权/版本/关键字筛选。
   */
  @GetMapping("/reports")
  public ApiResponse<?> reports(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int pageSize,
                                @RequestParam(defaultValue = "") String scene,
                                @RequestParam(defaultValue = "") String status,
                                @RequestParam(defaultValue = "") String licenseKey,
                                @RequestParam(defaultValue = "") String clientVersion,
                                @RequestParam(defaultValue = "") String keyword) {
    check(authorization);
    int safePage = Math.max(page, 1);
    int safePageSize = Math.min(Math.max(pageSize, 1), 100);
    var wrapper = Wrappers.lambdaQuery(SupportReport.class).orderByDesc(SupportReport::getId);
    if (!scene.isBlank()) wrapper.eq(SupportReport::getScene, scene.trim());
    if (!status.isBlank()) wrapper.eq(SupportReport::getStatus, status.trim());
    if (!licenseKey.isBlank()) wrapper.like(SupportReport::getLicenseKey, licenseKey.trim());
    if (!clientVersion.isBlank()) wrapper.like(SupportReport::getClientVersion, clientVersion.trim());
    if (!keyword.isBlank()) {
      String kw = keyword.trim();
      wrapper.and(item -> item.like(SupportReport::getReportNo, kw)
          .or().like(SupportReport::getErrorSummary, kw)
          .or().like(SupportReport::getHostname, kw));
    }
    return ApiResponse.ok(supportReportService.page(new Page<>(safePage, safePageSize), wrapper));
  }

  /**
   * 反馈详情。
   */
  @GetMapping("/reports/{id}")
  public ApiResponse<?> reportDetail(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable Long id) {
    check(authorization);
    SupportReport report = supportReportService.getById(id);
    if (report == null) throw new IllegalArgumentException("反馈记录不存在：" + id);
    return ApiResponse.ok(report);
  }

  /**
   * 更新反馈 —— 流转处理状态（submitted/triaged/resolved/ignored）或追加客服备注。
   */
  @PatchMapping("/reports/{id}")
  public ApiResponse<?> updateReport(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @PathVariable Long id, @RequestBody SupportReportUpdateRequest req) {
    check(authorization);
    return ApiResponse.ok(supportReportService.updateStatus(id, req.status(), req.adminNote()));
  }

  /**
   * 下载日志包 —— JWT 鉴权后返回 zip 附件；文件已被清理时返回 404。
   */
  @GetMapping("/reports/{id}/download")
  public ResponseEntity<Resource> downloadReport(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                @PathVariable Long id) {
    check(authorization);
    SupportReport report = supportReportService.getById(id);
    if (report == null) throw new IllegalArgumentException("反馈记录不存在：" + id);
    Path file = supportReportService.filePath(report);
    if (file == null) return ResponseEntity.notFound().build();
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.getReportNo() + ".zip\"")
        .body(new FileSystemResource(file));
  }

  /**
   * 鉴权校验 —— 验证 JWT Token，不通过则抛出异常。
   */
  private void check(String authorization) {
    String jwt = jwtService.extractBearerToken(authorization);
    if (jwtService.verifyAdminToken(jwt)) return;
    throw new IllegalArgumentException("Login expired or token invalid.");
  }
}
