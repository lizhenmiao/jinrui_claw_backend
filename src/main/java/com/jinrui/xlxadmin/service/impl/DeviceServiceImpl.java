package com.jinrui.xlxadmin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinrui.xlxadmin.dto.ClientDtos.DevicePingRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.LicenseCheckRequest;
import com.jinrui.xlxadmin.entity.DeviceRecord;
import com.jinrui.xlxadmin.entity.LicenseRecord;
import com.jinrui.xlxadmin.mapper.DeviceMapper;
import com.jinrui.xlxadmin.service.DeviceService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, DeviceRecord> implements DeviceService {

  @Autowired
  private DeviceMapper deviceMapper;

  @Override
  @Transactional
  public DeviceRecord recordLaunch(LicenseRecord license, LicenseCheckRequest req, HttpServletRequest http) {
    return record(license, req.usbId(), req.machineId(), req.hostname(), req.os(), req.arch(), req.driveFs(), req.clientVersion(), http);
  }

  @Override
  @Transactional
  public DeviceRecord recordPing(LicenseRecord license, DevicePingRequest req, HttpServletRequest http) {
    return record(license, req.usbId(), req.machineId(), req.hostname(), req.os(), req.arch(), req.driveFs(), req.clientVersion(), http);
  }

  @Override
  public IPage<Map<String, Object>> selectGroupedDevices(IPage<?> page, String licenseKey, String hostname,
                                                          String usbId, String lastIp) {
    return deviceMapper.selectGroupedDevices(page, licenseKey, hostname, usbId, lastIp);
  }

  private DeviceRecord record(LicenseRecord license, String usbId, String machineId, String hostname, String osName,
                              String arch, String driveFs, String clientVersion, HttpServletRequest http) {
    String safeMachineId = blankTo(machineId, "unknown");
    DeviceRecord device = getOne(Wrappers.lambdaQuery(DeviceRecord.class)
        .eq(DeviceRecord::getLicenseId, license.getId())
        .eq(DeviceRecord::getMachineId, safeMachineId)
        .last("limit 1"));
    if (device == null) device = new DeviceRecord();
    device.setLicenseId(license.getId());
    device.setUsbId(usbId);
    device.setMachineId(safeMachineId);
    device.setHostname(hostname);
    device.setOsName(osName);
    device.setArch(arch);
    device.setDriveFs(driveFs);
    device.setClientVersion(clientVersion);
    device.setLastIp(clientIp(http));
    device.setLastSeenAt(LocalDateTime.now());
    device.setLaunchCount(device.getLaunchCount() + 1);
    saveOrUpdate(device);
    return device;
  }

  private static String blankTo(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
    return request.getRemoteAddr();
  }
}
