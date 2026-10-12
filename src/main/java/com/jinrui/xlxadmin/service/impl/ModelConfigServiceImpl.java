package com.jinrui.xlxadmin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinrui.xlxadmin.dto.AdminDtos.ModelProviderSaveRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.ModelConfigRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.ModelConfigResult;
import com.jinrui.xlxadmin.entity.LicenseRecord;
import com.jinrui.xlxadmin.entity.ModelProvider;
import com.jinrui.xlxadmin.mapper.ModelProviderMapper;
import com.jinrui.xlxadmin.util.CryptoService;
import com.jinrui.xlxadmin.service.LicenseService;
import com.jinrui.xlxadmin.service.ModelConfigService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModelConfigServiceImpl extends ServiceImpl<ModelProviderMapper, ModelProvider> implements ModelConfigService {
  private static final TypeReference<List<Map<String, Object>>> MODEL_LIST_TYPE = new TypeReference<>() {};

  private final LicenseService licenseService;
  private final CryptoService cryptoService;
  private final ObjectMapper objectMapper;

  public ModelConfigServiceImpl(LicenseService licenseService,
                                CryptoService cryptoService, ObjectMapper objectMapper) {
    this.licenseService = licenseService;
    this.cryptoService = cryptoService;
    this.objectMapper = objectMapper;
  }

  @Override
  public ModelConfigResult getClientConfig(ModelConfigRequest req) {
    LicenseRecord license = licenseService.requireUsable(req.licenseKey(), req.usbId());
    String channel = blankTo(req.channel(), license.getChannel());
    String modelGroup = blankTo(license.getModelGroup(), "default");
    List<ModelProvider> providers = list(Wrappers.lambdaQuery(ModelProvider.class)
        .eq(ModelProvider::isEnabled, true)
        .eq(ModelProvider::getChannel, channel)
        .eq(ModelProvider::getModelGroup, modelGroup)
        .orderByAsc(ModelProvider::getId));

    int revision = 0;
    boolean forceDefault = false;
    String defaultModel = "";
    Map<String, Object> result = new LinkedHashMap<>();
    for (ModelProvider provider : providers) {
      List<Map<String, Object>> models = parseModels(provider.getModelsJson());
      if (models.isEmpty()) continue;
      revision = Math.max(revision, provider.getRevision());
      forceDefault = forceDefault || provider.isForceDefault();
      String itemDefault = provider.getProviderId() + "/" + blankTo(provider.getDefaultModel(), String.valueOf(models.get(0).get("id")));
      if (defaultModel.isBlank()) defaultModel = itemDefault;

      Map<String, Object> value = new LinkedHashMap<>();
      String keyMode = blankTo(provider.getKeyMode(), "client");
      value.put("displayName", provider.getDisplayName());
      value.put("api", provider.getApiType());
      value.put("baseUrl", provider.getBaseUrl());
      value.put("keyMode", keyMode);
      value.put("requiresClientKey", !"server".equalsIgnoreCase(keyMode));
      if ("server".equalsIgnoreCase(keyMode)) {
        value.put("apiKey", cryptoService.decrypt(provider.getApiKeyEncrypted()));
      }
      value.put("models", models);
      result.put(provider.getProviderId(), value);
    }
    return new ModelConfigResult(revision, defaultModel, forceDefault, result);
  }

  @Override
  @Transactional
  public ModelProvider save(Long id, ModelProviderSaveRequest req) {
    ModelProvider item = id == null ? new ModelProvider() : getById(id);
    if (item == null) throw new IllegalArgumentException("模型不存在");
    String providerId = blankTo(req.providerId(), item.getProviderId());
    if (providerId == null || providerId.isBlank()) {
      throw new IllegalArgumentException("Provider ID 不能为空");
    }
    ModelProvider existing = getOne(Wrappers.lambdaQuery(ModelProvider.class)
        .eq(ModelProvider::getProviderId, providerId)
        .last("limit 1"));
    if (existing != null && (id == null || !existing.getId().equals(id))) {
      throw new IllegalArgumentException("Provider ID 已存在，请编辑原模型或换一个 Provider ID：" + providerId);
    }
    item.setProviderId(providerId);
    item.setDisplayName(blankTo(req.displayName(), item.getDisplayName()));
    item.setApiType(blankTo(req.apiType(), "openai-completions"));
    item.setBaseUrl(blankTo(req.baseUrl(), item.getBaseUrl()));
    item.setKeyMode(normalizeKeyMode(req.keyMode()));
    if (req.apiKey() != null && !req.apiKey().isBlank() && !"********".equals(req.apiKey())) {
      item.setApiKeyEncrypted(cryptoService.encrypt(req.apiKey()));
    }
    item.setModelsJson(blankTo(req.modelsJson(), "[]"));
    item.setDefaultModel(req.defaultModel());
    item.setEnabled(req.enabled() == null || req.enabled());
    item.setChannel(blankTo(req.channel(), "stable"));
    item.setModelGroup(blankTo(req.modelGroup(), "default"));
    item.setForceDefault(Boolean.TRUE.equals(req.forceDefault()));
    item.setRevision(item.getRevision() + 1);
    if (item.getKeyMode() == null || item.getKeyMode().isBlank()) item.setKeyMode("client");
    saveOrUpdate(item);
    return item;
  }

  private List<Map<String, Object>> parseModels(String json) {
    try {
      if (json == null || json.isBlank()) return List.of();
      return objectMapper.readValue(json, MODEL_LIST_TYPE);
    } catch (Exception e) {
      return List.of();
    }
  }

  private static String blankTo(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private static String normalizeKeyMode(String value) {
    return "server".equalsIgnoreCase(value) ? "server" : "client";
  }
}
