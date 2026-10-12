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
    registry.addResourceHandler("/uploads/versions/**")
        .addResourceLocations(uploadDir.toUri().toString());
  }
}
