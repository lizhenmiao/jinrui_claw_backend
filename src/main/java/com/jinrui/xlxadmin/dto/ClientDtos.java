package com.jinrui.xlxadmin.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public final class ClientDtos {
  private ClientDtos() {}

  public record LicenseCheckRequest(
      @NotBlank String licenseKey,
      @NotBlank String usbId,
      String machineId,
      String hostname,
      String os,
      String arch,
      String driveFs,
      String clientVersion
  ) {}

  public record LicenseCheckResult(
      String status,
      boolean allowUpdate,
      String channel,
      String modelGroup
  ) {}

  public record DevicePingRequest(
      @NotBlank String licenseKey,
      @NotBlank String usbId,
      String machineId,
      String hostname,
      String os,
      String arch,
      String driveFs,
      String clientVersion
  ) {}

  public record UpdateCheckRequest(
      @NotBlank String licenseKey,
      String usbId,
      String platform,
      String arch,
      String currentVersion,
      String channel
  ) {}

  public record UpdateCheckResult(
      boolean needUpdate,
      String latestVersion,
      boolean force,
      String title,
      String downloadUrl,
      String sha256,
      List<String> releaseNotes,
      // 差量更新：zip 内 manifest.json 原文（原样透传，不重新计算 hash）；无差量能力时为 null，客户端自动回退全量
      @JsonRawValue String manifest,
      // 差量更新：按文件下载接口的基础 URL（不带 query），客户端拼接 ?version=&path=
      String fileUrlBase
  ) {}

  public record ModelConfigRequest(
      @NotBlank String licenseKey,
      String usbId,
      String clientVersion,
      String channel
  ) {}

  public record ModelConfigResult(
      int revision,
      String defaultModel,
      boolean forceDefault,
      Map<String, Object> providers
  ) {}

  public record ClientEventRequest(
      String licenseKey,
      String usbId,
      String machineId,
      String eventType,
      String level,
      String message,
      Map<String, Object> details
  ) {}
}
