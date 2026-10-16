package com.jinrui.xlxadmin.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadFileWebConfig implements WebMvcConfigurer {
  private final Path uploadDir;

  public UploadFileWebConfig(@Value("${xlx.upload-dir:data/uploads/versions}") String uploadDir) {
    this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Path.toUri() 仅在目录已存在时才带结尾斜杠；启动时上传目录尚未创建的话，
    // 缺少斜杠会导致 Spring 无法解析 /uploads/versions/** 下的资源（400 No static resource）
    String location = uploadDir.toUri().toString();
    if (!location.endsWith("/")) {
      location = location + "/";
    }
    registry.addResourceHandler("/uploads/versions/**")
        .addResourceLocations(location);
  }
}
