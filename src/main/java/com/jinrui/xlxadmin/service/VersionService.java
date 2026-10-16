package com.jinrui.xlxadmin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jinrui.xlxadmin.dto.AdminDtos.VersionSaveRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.UpdateCheckRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.UpdateCheckResult;
import com.jinrui.xlxadmin.entity.VersionInfo;
import jakarta.servlet.http.HttpServletRequest;

public interface VersionService extends IService<VersionInfo> {
  UpdateCheckResult checkUpdate(UpdateCheckRequest req, HttpServletRequest http);

  VersionInfo save(Long id, VersionSaveRequest req);
}
