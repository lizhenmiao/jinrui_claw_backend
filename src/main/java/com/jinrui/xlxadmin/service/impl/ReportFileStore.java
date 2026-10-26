package com.jinrui.xlxadmin.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 售后反馈日志包仓库 —— 客户端上传的诊断 zip 按月份分目录存储，供后台鉴权下载。
 *
 * 目录结构：{root}/{yyyyMM}/{reportNo}.zip
 * 与版本包不同，日志包不暴露免鉴权直链（内容含客户隐私）。
 */
@Service
public class ReportFileStore {
  private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

  private final Path rootDir;

  public ReportFileStore(@Value("${xlx.report-dir:data/uploads/reports}") String rootDir) {
    this.rootDir = Path.of(rootDir).toAbsolutePath().normalize();
  }

  /** 落盘结果：relativePath 相对仓库根目录（存库用），sha256 为十六进制小写。 */
  public record StoredFile(String relativePath, long size, String sha256) {}

  /** 保存 zip 并计算 SHA-256。 */
  public StoredFile save(InputStream in, String reportNo) throws IOException {
    String month = MONTH_FORMAT.format(LocalDate.now());
    String fileName = sanitize(reportNo) + ".zip";
    Path target = rootDir.resolve(month).resolve(fileName).normalize();
    if (!target.startsWith(rootDir)) {
      throw new IllegalArgumentException("回执码不合法：" + reportNo);
    }
    Files.createDirectories(target.getParent());
    MessageDigest digest = sha256Digest();
    long size;
    try (DigestInputStream digestIn = new DigestInputStream(in, digest)) {
      size = Files.copy(digestIn, target, StandardCopyOption.REPLACE_EXISTING);
    }
    return new StoredFile(month + "/" + fileName, size, toHex(digest.digest()));
  }

  /**
   * 按库中相对路径定位文件。路径非法（穿越/绝对路径）抛 IllegalArgumentException，
   * 文件不存在返回 null。
   */
  public Path resolve(String relativePath) {
    if (relativePath == null || relativePath.isBlank()) return null;
    String normalized = relativePath.replace('\\', '/');
    if (normalized.contains("..") || normalized.startsWith("/")) {
      throw new IllegalArgumentException("非法文件路径");
    }
    Path target = rootDir.resolve(normalized).normalize();
    if (!target.startsWith(rootDir) || !Files.isRegularFile(target)) {
      return null;
    }
    return target;
  }

  /** 落库失败等场景的补偿删除；失败不抛出、不阻断主流程。 */
  public void deleteQuietly(String relativePath) {
    try {
      Path target = resolve(relativePath);
      if (target != null) Files.deleteIfExists(target);
    } catch (Exception ignored) {
      // 补偿清理失败不阻断主流程
    }
  }

  private static MessageDigest sha256Digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private static String toHex(byte[] bytes) {
    char[] hex = new char[bytes.length * 2];
    for (int i = 0; i < bytes.length; i++) {
      hex[i * 2] = Character.forDigit((bytes[i] >> 4) & 0xF, 16);
      hex[i * 2 + 1] = Character.forDigit(bytes[i] & 0xF, 16);
    }
    return new String(hex);
  }

  private static String sanitize(String value) {
    String cleaned = value == null ? "" : value.trim().replaceAll("[^A-Za-z0-9._-]", "-");
    // 整段 "."/".." 会被 normalize 成上级目录，统一替换成 unknown 防越界
    if (cleaned.equals(".") || cleaned.equals("..")) return "unknown";
    return cleaned.isBlank() ? "unknown" : cleaned;
  }
}