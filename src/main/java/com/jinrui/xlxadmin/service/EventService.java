package com.jinrui.xlxadmin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jinrui.xlxadmin.dto.ClientDtos.ClientEventRequest;
import com.jinrui.xlxadmin.entity.ClientEvent;
import jakarta.servlet.http.HttpServletRequest;

public interface EventService extends IService<ClientEvent> {
  ClientEvent record(ClientEventRequest req, HttpServletRequest http);
}
