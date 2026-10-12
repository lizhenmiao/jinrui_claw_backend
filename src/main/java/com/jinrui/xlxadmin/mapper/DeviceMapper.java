package com.jinrui.xlxadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jinrui.xlxadmin.entity.DeviceRecord;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

public interface DeviceMapper extends BaseMapper<DeviceRecord> {
  IPage<Map<String, Object>> selectGroupedDevices(IPage<?> page,
                                                   @Param("licenseKey") String licenseKey,
                                                   @Param("hostname") String hostname,
                                                   @Param("usbId") String usbId,
                                                   @Param("lastIp") String lastIp);
}
