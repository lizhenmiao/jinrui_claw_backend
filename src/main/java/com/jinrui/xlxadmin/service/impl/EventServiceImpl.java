package com.jinrui.xlxadmin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinrui.xlxadmin.dto.ClientDtos.ClientEventRequest;
import com.jinrui.xlxadmin.entity.ClientEvent;
import com.jinrui.xlxadmin.entity.LicenseRecord;
import com.jinrui.xlxadmin.mapper.ClientEventMapper;
import com.jinrui.xlxadmin.service.EventService;
import com.jinrui.xlxadmin.service.LicenseService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class EventServiceImpl extends ServiceImpl<ClientEventMapper, ClientEvent> implements EventService {
  private final LicenseService licenseService;
  private final ObjectMapper objectMapper;

  public EventServiceImpl(LicenseService licenseService, ObjectMapper objectMapper) {
    this.licenseService = licenseService;
    this.objectMapper = objectMapper;
  }

  @Override
  public ClientEvent record(ClientEventRequest req, HttpServletRequest http) {
    ClientEvent event = new ClientEvent();
    event.setLicenseKey(req.licenseKey());
    LicenseRecord license = licenseService.getOne(Wrappers.lambdaQuery(LicenseRecord.class)
        .eq(LicenseRecord::getLicenseKey, req.licenseKey() == null ? "" : req.licenseKey())
        .last("limit 1"));
    if (license != null) event.setLicenseId(license.getId());
    event.setEventType(blankTo(req.eventType(), "client_event"));
    event.setLevel(blankTo(req.level(), "info"));
    event.setMessage(req.message());
    try {
      Map<String, Object> details = new LinkedHashMap<>();
      if (req.details() != null) details.putAll(req.details());
      putIfPresent(details, "usbId", req.usbId());
      putIfPresent(details, "machineId", req.machineId());
      putIfPresent(details, "ip", clientIp(http));
      event.setDetailsJson(objectMapper.writeValueAsString(details));
    } catch (Exception e) {
      event.setDetailsJson("{}");
    }
    save(event);
    return event;
  }

  private static String blankTo(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private static void putIfPresent(Map<String, Object> details, String key, String value) {
    if (value != null && !value.isBlank() && !details.containsKey(key)) details.put(key, value);
  }

  private static String clientIp(HttpServletRequest request) {
    if (request == null) return "";
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
    return request.getRemoteAddr();
  }
}
