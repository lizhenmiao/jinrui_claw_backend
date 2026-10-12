package com.jinrui.xlxadmin.controller;

import com.jinrui.xlxadmin.dto.ApiResponse;
import com.jinrui.xlxadmin.dto.ClientDtos.ClientEventRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.DevicePingRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.LicenseCheckRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.ModelConfigRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.UpdateCheckRequest;
import com.jinrui.xlxadmin.entity.LicenseRecord;
import com.jinrui.xlxadmin.service.DeviceService;
import com.jinrui.xlxadmin.service.EventService;
import com.jinrui.xlxadmin.service.LicenseService;
import com.jinrui.xlxadmin.service.ModelConfigService;
import com.jinrui.xlxadmin.service.VersionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

  public ClientController(LicenseService licenseService, DeviceService deviceService,
                          VersionService versionService, ModelConfigService modelConfigService,
                          EventService eventService) {
    this.licenseService = licenseService;
    this.deviceService = deviceService;
    this.versionService = versionService;
    this.modelConfigService = modelConfigService;
    this.eventService = eventService;
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
   * 检查更新 —— 根据授权通道返回最新版本和下载信息。
   */
  @PostMapping("/update/check")
  public ApiResponse<?> checkUpdate(@Valid @RequestBody UpdateCheckRequest req) {
    return ApiResponse.ok(versionService.checkUpdate(req));
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
}
