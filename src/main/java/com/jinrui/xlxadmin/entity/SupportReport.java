package com.jinrui.xlxadmin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("support_report")
public class SupportReport {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String reportNo;
  private Long licenseId;
  private Long deviceId;
  private String licenseKey;
  private String usbId;
  private String machineId;
  private String hostname;
  private String osName;
  private String arch;
  private String clientVersion;
  private String scene;
  private String errorCode;
  private String errorSummary;
  private String remark;
  private String contact;
  private String filePath;
  private Long fileSize;
  private String fileSha256;
  private String sourceIp;
  private String status;
  private String adminNote;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getReportNo() { return reportNo; }
  public void setReportNo(String reportNo) { this.reportNo = reportNo; }
  public Long getLicenseId() { return licenseId; }
  public void setLicenseId(Long licenseId) { this.licenseId = licenseId; }
  public Long getDeviceId() { return deviceId; }
  public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
  public String getLicenseKey() { return licenseKey; }
  public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }
  public String getUsbId() { return usbId; }
  public void setUsbId(String usbId) { this.usbId = usbId; }
  public String getMachineId() { return machineId; }
  public void setMachineId(String machineId) { this.machineId = machineId; }
  public String getHostname() { return hostname; }
  public void setHostname(String hostname) { this.hostname = hostname; }
  public String getOsName() { return osName; }
  public void setOsName(String osName) { this.osName = osName; }
  public String getArch() { return arch; }
  public void setArch(String arch) { this.arch = arch; }
  public String getClientVersion() { return clientVersion; }
  public void setClientVersion(String clientVersion) { this.clientVersion = clientVersion; }
  public String getScene() { return scene; }
  public void setScene(String scene) { this.scene = scene; }
  public String getErrorCode() { return errorCode; }
  public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
  public String getErrorSummary() { return errorSummary; }
  public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public String getContact() { return contact; }
  public void setContact(String contact) { this.contact = contact; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public Long getFileSize() { return fileSize; }
  public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
  public String getFileSha256() { return fileSha256; }
  public void setFileSha256(String fileSha256) { this.fileSha256 = fileSha256; }
  public String getSourceIp() { return sourceIp; }
  public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getAdminNote() { return adminNote; }
  public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}