package com.jinrui.xlxadmin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("model_provider")
public class ModelProvider {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String providerId;
  private String displayName;
  private String apiType = "openai-completions";
  private String baseUrl;
  private String apiKeyEncrypted;
  private String keyMode = "client";
  private String modelsJson;
  private String defaultModel;
  private boolean enabled = true;
  private String channel = "stable";
  private String modelGroup = "default";
  private boolean forceDefault;
  private int revision = 1;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getProviderId() { return providerId; }
  public void setProviderId(String providerId) { this.providerId = providerId; }
  public String getDisplayName() { return displayName; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }
  public String getApiType() { return apiType; }
  public void setApiType(String apiType) { this.apiType = apiType; }
  public String getBaseUrl() { return baseUrl; }
  public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
  public String getApiKeyEncrypted() { return apiKeyEncrypted; }
  public void setApiKeyEncrypted(String apiKeyEncrypted) { this.apiKeyEncrypted = apiKeyEncrypted; }
  public String getKeyMode() { return keyMode; }
  public void setKeyMode(String keyMode) { this.keyMode = keyMode; }
  public String getModelsJson() { return modelsJson; }
  public void setModelsJson(String modelsJson) { this.modelsJson = modelsJson; }
  public String getDefaultModel() { return defaultModel; }
  public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
  public String getChannel() { return channel; }
  public void setChannel(String channel) { this.channel = channel; }
  public String getModelGroup() { return modelGroup; }
  public void setModelGroup(String modelGroup) { this.modelGroup = modelGroup; }
  public boolean isForceDefault() { return forceDefault; }
  public void setForceDefault(boolean forceDefault) { this.forceDefault = forceDefault; }
  public int getRevision() { return revision; }
  public void setRevision(int revision) { this.revision = revision; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
