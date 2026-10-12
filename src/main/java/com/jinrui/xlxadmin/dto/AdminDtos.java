package com.jinrui.xlxadmin.dto;

import java.time.LocalDateTime;

public final class AdminDtos {
  private AdminDtos() {}

  public record AdminLoginRequest(
      String username,
      String password
  ) {}

  public record AdminLoginResult(
      String tokenType,
      String accessToken,
      long expiresIn,
      String username
  ) {}

  public record LicenseSaveRequest(
      String licenseKey,
      String customerName,
      String usbId,
      String status,
      LocalDateTime expiresAt,
      Boolean allowUpdate,
      String channel,
      String modelGroup
  ) {}

  public record VersionSaveRequest(
      String version,
      String platform,
      String arch,
      String channel,
      String title,
      String releaseNotes,
      String downloadUrl,
      String sha256,
      Boolean forceUpdate,
      Boolean published
  ) {}

  public record ModelProviderSaveRequest(
      String providerId,
      String displayName,
      String apiType,
      String baseUrl,
      String apiKey,
      String keyMode,
      String modelsJson,
      String defaultModel,
      Boolean enabled,
      String channel,
      String modelGroup,
      Boolean forceDefault
  ) {}
}
