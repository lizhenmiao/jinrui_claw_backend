package com.jinrui.xlxadmin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jinrui.xlxadmin.dto.AdminDtos.ModelProviderSaveRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.ModelConfigRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.ModelConfigResult;
import com.jinrui.xlxadmin.entity.ModelProvider;

public interface ModelConfigService extends IService<ModelProvider> {
  ModelConfigResult getClientConfig(ModelConfigRequest req);

  ModelProvider save(Long id, ModelProviderSaveRequest req);
}
