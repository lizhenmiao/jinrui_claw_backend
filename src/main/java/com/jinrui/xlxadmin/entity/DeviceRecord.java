package com.jinrui.xlxadmin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("devices")
public class DeviceRecord {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long licenseId;
  private String usbId;
  private String machineId;
  private String hostname;
  private String osName;
  private String arch;
  private String driveFs;
  private String clientVersion;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime firstSeenAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime lastSeenAt;

  private int launchCount;
  private String lastIp;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getLicenseId() { return licenseId; }
  public void setLicenseId(Long licenseId) { this.licenseId = licenseId; }
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
  public String getDriveFs() { return driveFs; }
  public void setDriveFs(String driveFs) { this.driveFs = driveFs; }
  public String getClientVersion() { return clientVersion; }
  public void setClientVersion(String clientVersion) { this.clientVersion = clientVersion; }
  public LocalDateTime getFirstSeenAt() { return firstSeenAt; }
  public void setFirstSeenAt(LocalDateTime firstSeenAt) { this.firstSeenAt = firstSeenAt; }
  public LocalDateTime getLastSeenAt() { return lastSeenAt; }
  public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
  public int getLaunchCount() { return launchCount; }
  public void setLaunchCount(int launchCount) { this.launchCount = launchCount; }
  public String getLastIp() { return lastIp; }
  public void setLastIp(String lastIp) { this.lastIp = lastIp; }
}
