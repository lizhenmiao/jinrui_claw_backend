package com.jinrui.xlxadmin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("version_info")
public class VersionInfo {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String version;
  private String platform = "windows";
  private String arch = "x64";
  private String channel = "stable";
  private String title;
  private String releaseNotes;
  private String downloadUrl;
  private String sha256;
  private boolean forceUpdate;
  private boolean published;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getVersion() { return version; }
  public void setVersion(String version) { this.version = version; }
  public String getPlatform() { return platform; }
  public void setPlatform(String platform) { this.platform = platform; }
  public String getArch() { return arch; }
  public void setArch(String arch) { this.arch = arch; }
  public String getChannel() { return channel; }
  public void setChannel(String channel) { this.channel = channel; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getReleaseNotes() { return releaseNotes; }
  public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }
  public String getDownloadUrl() { return downloadUrl; }
  public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
  public String getSha256() { return sha256; }
  public void setSha256(String sha256) { this.sha256 = sha256; }
  public boolean isForceUpdate() { return forceUpdate; }
  public void setForceUpdate(boolean forceUpdate) { this.forceUpdate = forceUpdate; }
  public boolean isPublished() { return published; }
  public void setPublished(boolean published) { this.published = published; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
