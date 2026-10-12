package com.jinrui.xlxadmin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("licenses")
public class LicenseRecord {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String licenseKey;
  private String customerName;
  private String usbId;
  private String status = "active";
  private LocalDateTime expiresAt;
  private boolean allowUpdate = true;
  private String channel = "stable";
  private String modelGroup = "default";

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getLicenseKey() { return licenseKey; }
  public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }
  public String getCustomerName() { return customerName; }
  public void setCustomerName(String customerName) { this.customerName = customerName; }
  public String getUsbId() { return usbId; }
  public void setUsbId(String usbId) { this.usbId = usbId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getExpiresAt() { return expiresAt; }
  public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
  public boolean isAllowUpdate() { return allowUpdate; }
  public void setAllowUpdate(boolean allowUpdate) { this.allowUpdate = allowUpdate; }
  public String getChannel() { return channel; }
  public void setChannel(String channel) { this.channel = channel; }
  public String getModelGroup() { return modelGroup; }
  public void setModelGroup(String modelGroup) { this.modelGroup = modelGroup; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
