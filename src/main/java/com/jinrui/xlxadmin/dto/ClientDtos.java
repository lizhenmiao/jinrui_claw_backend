package com.jinrui.xlxadmin.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
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

  /**
   * 一键提交日志的元数据（multipart 中的 meta part，JSON 序列化）。
   * 全部字段宽松处理：重置失败场景下客户端可能拿不到完整环境信息。
   */
  public record SupportReportMeta(
      // 客户端生成的回执码（≥8 位字母数字/短横线）；缺省时服务端兜底生成，但重试幂等依赖它
      String reportNo,
      // repair_failed/reset_failed/startup_failed/update_failed/manual
      String scene,
      String clientVersion,
      String os,
      String arch,
      String licenseKey,
      String usbId,
      String machineId,
      String hostname,
      String errorCode,
      String errorSummary,
      // 用户填写的问题描述
      String remark,
      // 用户留的联系方式（可选）
      String contact
  ) {}

  public record SupportReportResult(
      String reportNo,
      String status,
      LocalDateTime receivedAt,
      // true 表示该回执码此前已提交过（客户端重试命中幂等分支），未重复存储
      boolean duplicate,
      // 给客户端直接展示给用户的提示文案（含回执码）
      String supportHint
  ) {}
}
