package com.jinrui.xlxadmin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinrui.xlxadmin.dto.ClientDtos.DevicePingRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.LicenseCheckRequest;
import com.jinrui.xlxadmin.entity.DeviceRecord;
import com.jinrui.xlxadmin.entity.LicenseRecord;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface DeviceService extends IService<DeviceRecord> {
  DeviceRecord recordLaunch(LicenseRecord license, LicenseCheckRequest req, HttpServletRequest http);

  DeviceRecord recordPing(LicenseRecord license, DevicePingRequest req, HttpServletRequest http);

  IPage<Map<String, Object>> selectGroupedDevices(IPage<?> page, String licenseKey, String hostname,
                                                   String usbId, String lastIp);
}
