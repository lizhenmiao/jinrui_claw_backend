package com.jinrui.xlxadmin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinrui.xlxadmin.dto.ApiResponse;
import com.jinrui.xlxadmin.dto.ClientDtos.ClientEventRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.DevicePingRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.LicenseCheckRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.ModelConfigRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.SupportReportMeta;
import com.jinrui.xlxadmin.dto.ClientDtos.UpdateCheckRequest;
import com.jinrui.xlxadmin.entity.LicenseRecord;
import com.jinrui.xlxadmin.service.DeviceService;
import com.jinrui.xlxadmin.service.EventService;
import com.jinrui.xlxadmin.service.LicenseService;
import com.jinrui.xlxadmin.service.ModelConfigService;
import com.jinrui.xlxadmin.service.SupportReportService;
import com.jinrui.xlxadmin.service.VersionService;
import com.jinrui.xlxadmin.service.impl.VersionFileStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 客户端接口 —— 给小龙虾U盘程序提供授权校验、心跳、更新检查、模型同步、事件上报。
 */
@RestController
@RequestMapping("/api/client")
public class ClientController {
  private final LicenseService licenseService;
  private final DeviceService deviceService;
  private final VersionService versionService;
  private final ModelConfigService modelConfigService;
  private final EventService eventService;
  private final SupportReportService supportReportService;
  private final VersionFileStore versionFileStore;
  private final ObjectMapper objectMapper;

  public ClientController(LicenseService licenseService, DeviceService deviceService,
                          VersionService versionService, ModelConfigService modelConfigService,
                          EventService eventService, SupportReportService supportReportService,
                          VersionFileStore versionFileStore, ObjectMapper objectMapper) {
    this.licenseService = licenseService;
    this.deviceService = deviceService;
    this.versionService = versionService;
    this.modelConfigService = modelConfigService;
    this.eventService = eventService;
    this.supportReportService = supportReportService;
    this.versionFileStore = versionFileStore;
    this.objectMapper = objectMapper;
  }

  /**
   * 启动授权校验 —— 校验授权码、绑定U盘ID，并记录设备启动信息。
   */
  @PostMapping("/license/check")
  public ApiResponse<?> checkLicense(@Valid @RequestBody LicenseCheckRequest req, HttpServletRequest http) {
    return licenseService.checkLicense(req, http);
  }

  /**
   * 设备心跳 —— 记录客户端最近一次启动/在线状态。
   */
  @PostMapping("/device/ping")
  public ApiResponse<?> ping(@Valid @RequestBody DevicePingRequest req, HttpServletRequest http) {
    LicenseRecord license = licenseService.requireUsable(req.licenseKey(), req.usbId());
    return ApiResponse.ok(deviceService.recordPing(license, req, http));
  }

  /**
   * 检查更新 —— 根据授权通道返回最新版本和下载信息；命中新版本时附带差量更新所需的
   * manifest 原文和按文件下载基础 URL（fileUrlBase）。
   */
  @PostMapping("/update/check")
  public ApiResponse<?> checkUpdate(@Valid @RequestBody UpdateCheckRequest req, HttpServletRequest http) {
    return ApiResponse.ok(versionService.checkUpdate(req, http));
  }

  /**
   * 差量更新按文件下载 —— 客户端拼 ?version=&path= 拉取单个变更文件，免鉴权直链。
   * platform/arch 由后台编进 fileUrlBase 路径段，避免同版本号跨平台互相覆盖。
   * path 已由 Spring 完成 URL 解码；含 ".." 或绝对路径时拒绝（400），文件不存在返回 404。
   */
  @GetMapping("/versions/file/{platform}/{arch}")
  public ResponseEntity<Resource> downloadVersionFile(@PathVariable("platform") String platform,
                                                      @PathVariable("arch") String arch,
                                                      @RequestParam("version") String version,
                                                      @RequestParam("path") String path) {
    Path file = versionFileStore.resolveFile(platform, arch, version, path);
    if (file == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
        .body(new FileSystemResource(file));
  }

  /**
   * 同步模型 —— 下发后台配置的内置模型和默认模型。
   */
  @PostMapping("/models/config")
  public ApiResponse<?> modelConfig(@Valid @RequestBody ModelConfigRequest req) {
    return ApiResponse.ok(modelConfigService.getClientConfig(req));
  }

  /**
   * 事件上报 —— 记录启动失败、微信超时、QQ离线等售后诊断日志。
   */
  @PostMapping("/event")
  public ApiResponse<?> event(@RequestBody ClientEventRequest req, HttpServletRequest http) {
    return ApiResponse.ok(eventService.record(req, http));
  }

  /**
   * 一键提交日志 —— 客户端启动修复/重置失败时上传诊断日志包
   * （multipart：file = zip 日志包，meta = JSON 元数据，字段见 ClientDtos.SupportReportMeta）。
   * 宽松校验：不检查授权可用状态，用可选 HMAC 签名 / 频率限流 / 回执码幂等兜底。
   */
  @PostMapping(value = "/report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<?> submitReport(@RequestParam(value = "meta", required = false) String metaJson,
                                     @RequestParam("file") MultipartFile file,
                                     HttpServletRequest http) {
    return ApiResponse.ok("日志已提交", supportReportService.submit(parseReportMeta(metaJson), file, http));
  }

  /** meta 缺省时按空元数据兜底（保证最坏情况下也能把日志传上来）；提供但非法则报错便于联调。 */
  private SupportReportMeta parseReportMeta(String metaJson) {
    if (metaJson == null || metaJson.isBlank()) return null;
    // Windows 侧写出的 JSON 常带 UTF-8 BOM，Jackson 会直接报错，先剥离
    String payload = metaJson.charAt(0) == '\uFEFF' ? metaJson.substring(1) : metaJson;
    try {
      return objectMapper.readValue(payload, SupportReportMeta.class);
    } catch (Exception e) {
      throw new IllegalArgumentException("meta 不是合法 JSON：" + e.getMessage());
    }
  }
}
