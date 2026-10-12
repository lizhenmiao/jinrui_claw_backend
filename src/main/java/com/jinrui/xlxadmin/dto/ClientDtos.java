package com.jinrui.xlxadmin.dto;

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
      List<String> releaseNotes
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
