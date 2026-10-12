package com.jinrui.xlxadmin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jinrui.xlxadmin.dto.AdminDtos.LicenseSaveRequest;
import com.jinrui.xlxadmin.dto.ApiResponse;
import com.jinrui.xlxadmin.dto.ClientDtos.LicenseCheckRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.LicenseCheckResult;
import com.jinrui.xlxadmin.entity.LicenseRecord;
import jakarta.servlet.http.HttpServletRequest;

public interface LicenseService extends IService<LicenseRecord> {
  ApiResponse<LicenseCheckResult> checkLicense(LicenseCheckRequest req, HttpServletRequest http);

  LicenseRecord requireUsable(String licenseKey, String usbId);

  ApiResponse<LicenseCheckResult> validateUsable(LicenseRecord license, String usbId);

  int expireOverdueLicenses();

  LicenseRecord save(Long id, LicenseSaveRequest req);
}
