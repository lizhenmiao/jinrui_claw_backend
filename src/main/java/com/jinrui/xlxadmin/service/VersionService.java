package com.jinrui.xlxadmin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jinrui.xlxadmin.dto.AdminDtos.VersionSaveRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.UpdateCheckRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.UpdateCheckResult;
import com.jinrui.xlxadmin.entity.VersionInfo;

public interface VersionService extends IService<VersionInfo> {
  UpdateCheckResult checkUpdate(UpdateCheckRequest req);

  VersionInfo save(Long id, VersionSaveRequest req);
}
