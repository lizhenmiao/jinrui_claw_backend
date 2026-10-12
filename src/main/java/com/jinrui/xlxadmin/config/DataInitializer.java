package com.jinrui.xlxadmin.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jinrui.xlxadmin.entity.LicenseRecord;
import com.jinrui.xlxadmin.entity.ModelProvider;
import com.jinrui.xlxadmin.entity.VersionInfo;
import com.jinrui.xlxadmin.service.LicenseService;
import com.jinrui.xlxadmin.service.ModelConfigService;
import com.jinrui.xlxadmin.service.VersionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
  private final LicenseService licenseService;
  private final VersionService versionService;
  private final ModelConfigService modelConfigService;

  public DataInitializer(LicenseService licenseService, VersionService versionService,
                         ModelConfigService modelConfigService) {
    this.licenseService = licenseService;
    this.versionService = versionService;
    this.modelConfigService = modelConfigService;
  }

  @Override
  public void run(String... args) {
    LicenseRecord demo = licenseService.getOne(Wrappers.lambdaQuery(LicenseRecord.class)
        .eq(LicenseRecord::getLicenseKey, "XLX-DEMO-0001")
        .last("limit 1"));
    if (demo == null) {
      LicenseRecord item = new LicenseRecord();
      item.setLicenseKey("XLX-DEMO-0001");
      item.setCustomerName("演示客户");
      item.setStatus("active");
      item.setChannel("stable");
      item.setModelGroup("default");
      licenseService.save(item);
    }

    if (versionService.count() == 0) {
      VersionInfo version = new VersionInfo();
      version.setVersion("2026.7.24");
      version.setChannel("stable");
      version.setTitle("小龙虾U盘版 2026.7.24");
      version.setReleaseNotes("初始化后台版本\n支持授权、设备、版本、模型和事件管理");
      version.setDownloadUrl("");
      version.setPublished(true);
      versionService.save(version);
    }

    ModelProvider existing = modelConfigService.getOne(Wrappers.lambdaQuery(ModelProvider.class)
        .eq(ModelProvider::getProviderId, "jinrui-deepseek")
        .last("limit 1"));
    if (existing == null) {
      ModelProvider provider = new ModelProvider();
      provider.setProviderId("jinrui-deepseek");
      provider.setDisplayName("锦锐内置算力");
      provider.setApiType("openai-completions");
      provider.setBaseUrl("https://cloud.tejinrui.com/api/balance/v1");
      provider.setKeyMode("client");
      provider.setModelsJson("[{\"id\":\"deepseek-v4-flash\",\"name\":\"DeepSeek V4 Flash\",\"reasoning\":true,\"input\":[\"text\"],\"contextWindow\":128000,\"maxTokens\":8192},{\"id\":\"deepseek-v4-pro\",\"name\":\"DeepSeek V4 Pro\",\"reasoning\":true,\"input\":[\"text\"],\"contextWindow\":128000,\"maxTokens\":8192}]");
      provider.setDefaultModel("deepseek-v4-flash");
      provider.setEnabled(true);
      provider.setChannel("stable");
      provider.setModelGroup("default");
      provider.setRevision(1);
      modelConfigService.save(provider);
    }
  }
}
