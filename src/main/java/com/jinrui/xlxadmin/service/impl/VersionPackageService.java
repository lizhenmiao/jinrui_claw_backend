package com.jinrui.xlxadmin.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VersionPackageService {
  private static final DateTimeFormatter NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final Path uploadDir;
  private final String publicBaseUrl;

  public VersionPackageService(@Value("${xlx.upload-dir:data/uploads/versions}") String uploadDir,
                               @Value("${xlx.public-base-url:}") String publicBaseUrl) {
    this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
  }

  public Map<String, Object> upload(MultipartFile file, HttpServletRequest request) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("请选择要上传的更新包");
    }
    String originalName = sanitize(file.getOriginalFilename());
    if (!originalName.toLowerCase(Locale.ROOT).endsWith(".zip") && !originalName.toLowerCase(Locale.ROOT).endsWith(".rar")) {
      throw new IllegalArgumentException("目前只允许上传 .zip 或 .rar 更新包");
    }

    try {
      Files.createDirectories(uploadDir);
      String storedName = buildStoredName(originalName);
      Path target = uploadDir.resolve(storedName).normalize();
      if (!target.startsWith(uploadDir)) {
        throw new IllegalArgumentException("文件名不合法");
      }

      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      long size = 0;
      try (InputStream input = file.getInputStream(); OutputStream output = Files.newOutputStream(target)) {
        byte[] buffer = new byte[1024 * 1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
          digest.update(buffer, 0, read);
          output.write(buffer, 0, read);
          size += read;
        }
      }

      String sha256 = HexFormat.of().formatHex(digest.digest());
      String downloadUrl = buildDownloadUrl(request, storedName);
      return Map.of(
          "originalName", originalName,
          "fileName", storedName,
          "size", size,
          "sha256", sha256,
          "downloadUrl", downloadUrl
      );
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("上传更新包失败：" + e.getMessage(), e);
    }
  }

  private String buildStoredName(String originalName) {
    String base = originalName.substring(0, originalName.length() - 4);
    if (base.isBlank()) base = "openclaw-update";
    String shortId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    return base + "-" + LocalDateTime.now().format(NAME_TIME) + "-" + shortId + ".zip";
  }

  private String buildDownloadUrl(HttpServletRequest request, String storedName) {
    String base = publicBaseUrl.isBlank() ? requestBaseUrl(request) : publicBaseUrl;
    return trimRightSlash(base) + "/uploads/versions/" + storedName;
  }

  private String requestBaseUrl(HttpServletRequest request) {
    String forwardedProto = request.getHeader("X-Forwarded-Proto");
    String forwardedHost = request.getHeader("X-Forwarded-Host");
    String proto = forwardedProto == null || forwardedProto.isBlank() ? request.getScheme() : forwardedProto.split(",")[0].trim();
    String host = forwardedHost == null || forwardedHost.isBlank() ? request.getServerName() : forwardedHost.split(",")[0].trim();
    int port = request.getServerPort();
    String portPart = "";
    if ((proto.equalsIgnoreCase("http") && port != 80) || (proto.equalsIgnoreCase("https") && port != 443)) {
      portPart = ":" + port;
    }
    return proto + "://" + host + portPart + request.getContextPath();
  }

  private String sanitize(String name) {
    String cleaned = name == null ? "update.zip" : Path.of(name).getFileName().toString();
    cleaned = cleaned.replaceAll("[\\\\/:*?\"<>|\\s]+", "-");
    cleaned = cleaned.replaceAll("[^A-Za-z0-9._-]", "-");
    while (cleaned.contains("--")) cleaned = cleaned.replace("--", "-");
    return cleaned.isBlank() ? "update.zip" : cleaned;
  }

  private String trimRightSlash(String value) {
    String result = value;
    while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
    return result;
  }
}
