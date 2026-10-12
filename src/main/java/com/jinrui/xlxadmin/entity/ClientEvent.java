package com.jinrui.xlxadmin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("client_event")
public class ClientEvent {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long licenseId;
  private Long deviceId;
  private String licenseKey;
  private String eventType;
  private String level;
  private String message;
  private String detailsJson;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getLicenseId() { return licenseId; }
  public void setLicenseId(Long licenseId) { this.licenseId = licenseId; }
  public Long getDeviceId() { return deviceId; }
  public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
  public String getLicenseKey() { return licenseKey; }
  public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }
  public String getEventType() { return eventType; }
  public void setEventType(String eventType) { this.eventType = eventType; }
  public String getLevel() { return level; }
  public void setLevel(String level) { this.level = level; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
  public String getDetailsJson() { return detailsJson; }
  public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
