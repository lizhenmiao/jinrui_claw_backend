package com.jinrui.xlxadmin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinrui.xlxadmin.dto.AdminDtos.LicenseSaveRequest;
import com.jinrui.xlxadmin.dto.ApiResponse;
import com.jinrui.xlxadmin.dto.ClientDtos.LicenseCheckRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.LicenseCheckResult;
import com.jinrui.xlxadmin.entity.LicenseRecord;
import com.jinrui.xlxadmin.mapper.LicenseMapper;
import com.jinrui.xlxadmin.service.DeviceService;
import com.jinrui.xlxadmin.service.LicenseService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LicenseServiceImpl extends ServiceImpl<LicenseMapper, LicenseRecord> implements LicenseService {
  private final DeviceService deviceService;

  public LicenseServiceImpl(DeviceService deviceService) {
    this.deviceService = deviceService;
  }

  @Override
  @Transactional
  public ApiResponse<LicenseCheckResult> checkLicense(LicenseCheckRequest req, HttpServletRequest http) {
    expireOverdueLicenses();
    LicenseRecord license = getOne(Wrappers.lambdaQuery(LicenseRecord.class)
        .eq(LicenseRecord::getLicenseKey, req.licenseKey())
        .last("limit 1"));
    if (license == null) return ApiResponse.fail("LICENSE_NOT_FOUND", "授权不存在");
    ApiResponse<LicenseCheckResult> usable = validateUsable(license, req.usbId());
    if (!usable.ok()) return usable;

    if (license.getUsbId() == null || license.getUsbId().isBlank()) {
      license.setUsbId(req.usbId());
      saveOrUpdate(license);
    }
    deviceService.recordLaunch(license, req, http);
    return ApiResponse.ok("授权正常", new LicenseCheckResult(
        license.getStatus(),
        license.isAllowUpdate(),
        license.getChannel(),
        license.getModelGroup()
    ));
  }

  @Override
  public LicenseRecord requireUsable(String licenseKey, String usbId) {
    expireOverdueLicenses();
    LicenseRecord license = getOne(Wrappers.lambdaQuery(LicenseRecord.class)
        .eq(LicenseRecord::getLicenseKey, licenseKey)
        .last("limit 1"));
    if (license == null) throw new IllegalArgumentException("授权不存在");
    ApiResponse<LicenseCheckResult> result = validateUsable(license, usbId);
    if (!result.ok()) throw new IllegalStateException(result.message());
    return license;
  }

  @Override
  public ApiResponse<LicenseCheckResult> validateUsable(LicenseRecord license, String usbId) {
    if ("expired".equalsIgnoreCase(license.getStatus()) || isExpired(license)) {
      markExpiredIfNeeded(license);
      return ApiResponse.fail("LICENSE_EXPIRED", "授权已过期");
    }
    if (!"active".equalsIgnoreCase(license.getStatus())) {
      return ApiResponse.fail("LICENSE_DISABLED", "授权已禁用");
    }
    if (license.getExpiresAt() != null && license.getExpiresAt().isBefore(LocalDateTime.now())) {
      return ApiResponse.fail("LICENSE_EXPIRED", "授权已过期");
    }
    if (license.getUsbId() != null && !license.getUsbId().isBlank() && usbId != null && !usbId.isBlank()
        && !license.getUsbId().equals(usbId)) {
      return ApiResponse.fail("USB_MISMATCH", "授权与当前U盘不匹配");
    }
    return ApiResponse.ok(new LicenseCheckResult(license.getStatus(), license.isAllowUpdate(), license.getChannel(), license.getModelGroup()));
  }

  @Override
  @Transactional
  public LicenseRecord save(Long id, LicenseSaveRequest req) {
    LicenseRecord item = id == null ? new LicenseRecord() : getById(id);
    if (item == null && id != null) throw new IllegalArgumentException("授权不存在");
    if (item == null) item = new LicenseRecord();
    item.setLicenseKey(blankTo(req.licenseKey(), id == null ? "XLX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() : item.getLicenseKey()));
    item.setCustomerName(req.customerName());
    item.setUsbId(req.usbId());
    item.setExpiresAt(req.expiresAt());
    item.setStatus(normalizeStatusByExpiry(blankTo(req.status(), "active"), item.getExpiresAt()));
    item.setAllowUpdate(req.allowUpdate() == null || req.allowUpdate());
    item.setChannel(blankTo(req.channel(), "stable"));
    item.setModelGroup(blankTo(req.modelGroup(), "default"));
    saveOrUpdate(item);
    markExpiredIfNeeded(item);
    return item;
  }

  @Override
  @Transactional
  public int expireOverdueLicenses() {
    return getBaseMapper().update(null, Wrappers.lambdaUpdate(LicenseRecord.class)
        .eq(LicenseRecord::getStatus, "active")
        .isNotNull(LicenseRecord::getExpiresAt)
        .lt(LicenseRecord::getExpiresAt, LocalDateTime.now())
        .set(LicenseRecord::getStatus, "expired"));
  }

  private void markExpiredIfNeeded(LicenseRecord license) {
    if (license != null && "active".equalsIgnoreCase(license.getStatus()) && isExpired(license)) {
      license.setStatus("expired");
      updateById(license);
    }
  }

  private static boolean isExpired(LicenseRecord license) {
    return license.getExpiresAt() != null && license.getExpiresAt().isBefore(LocalDateTime.now());
  }

  private static String normalizeStatusByExpiry(String status, LocalDateTime expiresAt) {
    if ("expired".equalsIgnoreCase(status) && (expiresAt == null || !expiresAt.isBefore(LocalDateTime.now()))) {
      return "active";
    }
    if ("active".equalsIgnoreCase(status) && expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
      return "expired";
    }
    return status;
  }

  private static String blankTo(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
