package com.jinrui.xlxadmin.controller;

import com.jinrui.xlxadmin.dto.AdminDtos.AdminLoginRequest;
import com.jinrui.xlxadmin.dto.AdminDtos.AdminLoginResult;
import com.jinrui.xlxadmin.dto.ApiResponse;
import com.jinrui.xlxadmin.util.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台登录接口 —— 用户名密码验证通过后签发 JWT Token。
 */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
  private final String adminUsername;
  private final String adminPassword;
  private final JwtService jwtService;

  public AdminAuthController(@Value("${xlx.admin-username}") String adminUsername,
                             @Value("${xlx.admin-password}") String adminPassword,
                             JwtService jwtService) {
    this.adminUsername = adminUsername;
    this.adminPassword = adminPassword;
    this.jwtService = jwtService;
  }

  /**
   * 登录校验 —— 目前使用配置文件中的账号密码，后续可改为 admin_user 表。
   */
  @PostMapping("/login")
  public ApiResponse<?> login(@RequestBody AdminLoginRequest req) {
    if (req == null || !adminUsername.equals(req.username()) || !adminPassword.equals(req.password())) {
      return ApiResponse.fail("LOGIN_FAILED", "用户名或密码不正确");
    }
    String token = jwtService.createAdminToken(adminUsername);
    return ApiResponse.ok("登录成功", new AdminLoginResult("Bearer", token, jwtService.ttlSeconds(), adminUsername));
  }
}
