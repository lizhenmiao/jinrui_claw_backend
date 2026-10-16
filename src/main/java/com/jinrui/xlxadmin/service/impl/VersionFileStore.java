package com.jinrui.xlxadmin.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 差量更新文件仓库 —— 上传的 zip 解包后先暂存，创建/编辑版本记录时按
 * platform/arch/version 三维隔离绑定，供客户端按文件下载。
 *
 * 目录结构：
 *   {root}/staging/{zip存储名}/manifest.json + files/{相对路径}   （上传后暂存，绑定后删除）
 *   {root}/{platform}/{arch}/{version}/manifest.json + files/...   （绑定后的正式目录）
 *
 * 两段式的原因：上传 zip 时还不知道 platform/arch（建版本记录时才填），
 * 而客户端下载契约只携带 version+path，platform/arch 由后台编进 fileUrlBase 路径。
 * zip 内没有 manifest.json 时不做任何解包，该版本只支持全量更新。
 */
@Service
public class VersionFileStore {
  private static final String MANIFEST_NAME = "manifest.json";
  private static final String FILES_DIR = "files";
  private static final String STAGING_DIR = "staging";

  private final Path rootDir;
  private final ObjectMapper objectMapper;
  /** 允许差量更新的平台白名单（逗号分隔）；非白名单平台只走全量 zip。 */
  private final Set<String> deltaPlatforms;

  public VersionFileStore(@Value("${xlx.version-files-dir:data/uploads/version-files}") String rootDir,
                          @Value("${xlx.delta-enabled-platforms:windows}") String deltaPlatforms,
                          ObjectMapper objectMapper) {
    this.rootDir = Path.of(rootDir).toAbsolutePath().normalize();
    this.deltaPlatforms = Stream.of(deltaPlatforms.split(","))
        .map(String::trim).filter(s -> !s.isEmpty()).map(s -> s.toLowerCase(Locale.ROOT))
        .collect(Collectors.toUnmodifiableSet());
    this.objectMapper = objectMapper;
  }

  /** 该平台是否在差量更新白名单内。 */
  public boolean isDeltaPlatform(String platform) {
    return platform != null && deltaPlatforms.contains(platform.trim().toLowerCase(Locale.ROOT));
  }

  /** 解包结果：manifest 里的版本号、文件数、暂存目录键（= zip 存储名去扩展名）。 */
  public record ExtractResult(String version, int fileCount, String stagingKey) {}

  /**
   * 解包 zip 到暂存目录。manifest.json 原样保存（不重新计算 hash）。
   * 找不到 manifest.json 时返回 null（仅支持全量更新）。
   */
  public ExtractResult extractAndStore(Path zipPath, String storedName) throws IOException {
    try (ZipFile zip = new ZipFile(zipPath.toFile())) {
      ZipEntry manifestEntry = findManifestEntry(zip);
      if (manifestEntry == null) {
        return null;
      }
      String topDir = manifestEntry.getName().substring(0, manifestEntry.getName().length() - MANIFEST_NAME.length());
      String manifestRaw;
      try (InputStream in = zip.getInputStream(manifestEntry)) {
        manifestRaw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }
      JsonNode manifestNode = objectMapper.readTree(manifestRaw);
      JsonNode versionNode = manifestNode.get("version");
      if (versionNode == null || versionNode.asText().isBlank()) {
        throw new IllegalArgumentException("manifest.json 缺少 version 字段");
      }
      String version = sanitize(versionNode.asText());
      JsonNode filesNode = manifestNode.get("files");
      int fileCount = filesNode != null && filesNode.isObject() ? filesNode.size() : 0;

      String stagingKey = sanitize(storedName.endsWith(".zip")
          ? storedName.substring(0, storedName.length() - 4) : storedName);
      Path stagingDir = rootDir.resolve(STAGING_DIR).resolve(stagingKey).normalize();
      if (!stagingDir.startsWith(rootDir)) {
        throw new IllegalArgumentException("zip 存储名不合法：" + storedName);
      }
      deleteRecursively(stagingDir);
      Path filesDir = stagingDir.resolve(FILES_DIR);
      Files.createDirectories(filesDir);
      Files.writeString(stagingDir.resolve(MANIFEST_NAME), manifestRaw, StandardCharsets.UTF_8);

      // 解包顶层目录下的全部文件；manifest.json 自身不放进 files/（它不在 files 清单里）
      Enumeration<? extends ZipEntry> entries = zip.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        String name = entry.getName();
        if (entry.isDirectory() || !name.startsWith(topDir) || name.equals(manifestEntry.getName())) {
          continue;
        }
        String relative = name.substring(topDir.length());
        Path target = filesDir.resolve(relative).normalize();
        if (!target.startsWith(filesDir)) {
          throw new IllegalArgumentException("zip 内存在非法路径：" + name);
        }
        Files.createDirectories(target.getParent() == null ? filesDir : target.getParent());
        try (InputStream in = zip.getInputStream(entry)) {
          Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
      }
      return new ExtractResult(version, fileCount, stagingKey);
    }
  }

  /**
   * 创建/编辑版本记录时调用：按 downloadUrl 里的 zip 存储名找到暂存目录，
   * 移动绑定到 {platform}/{arch}/{version}/。
   * 返回 true 表示完成绑定（或已绑定过、幂等跳过）；暂存不存在时返回 false（仅全量版本）。
   * 目标目录已存在且 manifest 不一致时抛 IllegalArgumentException，避免跨平台/重复上传静默覆盖。
   */
  public boolean bind(String downloadUrl, String platform, String arch, String version) throws IOException {
    String stagingKey = stagingKeyFromDownloadUrl(downloadUrl);
    if (stagingKey == null) {
      return false;
    }
    Path stagingDir = rootDir.resolve(STAGING_DIR).resolve(stagingKey).normalize();
    if (!stagingDir.startsWith(rootDir) || !Files.isDirectory(stagingDir)) {
      return false;
    }
    // 非差量白名单平台（如 mac 全量整包）不绑定，顺手清掉暂存防磁盘泄漏。
    // 旧包若带 manifest（历史遗留）也在此丢弃；平台将来进白名单后重新上传即可。
    if (!isDeltaPlatform(platform)) {
      deleteRecursively(stagingDir);
      return false;
    }
    Path targetDir = versionDir(platform, arch, version);
    if (Files.isDirectory(targetDir)) {
      String boundManifest = readManifestRaw(platform, arch, version);
      if (boundManifest == null) {
        // 目标目录存在但没有 manifest（如历史记录被手动清理过一半），视为未完成绑定，重建
        deleteRecursively(targetDir);
      } else {
        String stagedManifest = Files.readString(stagingDir.resolve(MANIFEST_NAME), StandardCharsets.UTF_8);
        if (!stagedManifest.equals(boundManifest)) {
          throw new IllegalArgumentException(
              "版本文件冲突：" + platform + "/" + arch + "/" + version
                  + " 已绑定过内容不同的更新包，请换版本号或先删除旧版本记录");
        }
        // 内容一致的重复绑定（如再次编辑保存）：幂等，仅清理暂存
        deleteRecursively(stagingDir);
        return true;
      }
    }
    Files.createDirectories(targetDir.getParent());
    Files.move(stagingDir, targetDir);
    return true;
  }

  /** 读取指定 platform/arch/version 的 manifest.json 原文；不存在返回 null。 */
  public String readManifestRaw(String platform, String arch, String version) {
    if (isBlank(platform) || isBlank(arch) || isBlank(version)) return null;
    Path manifest = versionDir(platform, arch, version).resolve(MANIFEST_NAME);
    if (!Files.isRegularFile(manifest)) return null;
    try {
      return Files.readString(manifest, StandardCharsets.UTF_8);
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * 按 platform/arch/version + 相对路径定位文件。路径非法（穿越/绝对路径）抛
   * IllegalArgumentException，文件不存在返回 null。
   */
  public Path resolveFile(String platform, String arch, String version, String relativePath) {
    if (isBlank(version) || isBlank(relativePath)) {
      return null;
    }
    String normalized = relativePath.replace('\\', '/');
    if (normalized.contains("..") || normalized.startsWith("/")) {
      throw new IllegalArgumentException("非法文件路径");
    }
    Path filesDir = versionDir(platform, arch, version).resolve(FILES_DIR);
    Path target = filesDir.resolve(normalized).normalize();
    if (!target.startsWith(filesDir) || !Files.isRegularFile(target)) {
      return null;
    }
    return target;
  }

  private Path versionDir(String platform, String arch, String version) {
    Path dir = rootDir.resolve(sanitize(platform)).resolve(sanitize(arch)).resolve(sanitize(version)).normalize();
    if (!dir.startsWith(rootDir)) {
      throw new IllegalArgumentException("非法版本定位参数");
    }
    return dir;
  }

  /** 从 downloadUrl 提取 zip 存储名并转成暂存键；URL 不属于本后台时返回 null。 */
  private static String stagingKeyFromDownloadUrl(String downloadUrl) {
    if (isBlank(downloadUrl)) return null;
    String path = downloadUrl;
    int query = path.indexOf('?');
    if (query >= 0) path = path.substring(0, query);
    int slash = path.lastIndexOf('/');
    String fileName = slash >= 0 ? path.substring(slash + 1) : path;
    if (!fileName.toLowerCase(Locale.ROOT).endsWith(".zip")) return null;
    return sanitize(fileName.substring(0, fileName.length() - 4));
  }

  private static ZipEntry findManifestEntry(ZipFile zip) {
    Enumeration<? extends ZipEntry> entries = zip.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      String name = entry.getName();
      if (entry.isDirectory() || !name.endsWith("/" + MANIFEST_NAME)) {
        continue;
      }
      // 仅认顶层目录下的 manifest.json（如 zgyclaw/manifest.json）
      if (name.indexOf('/') == name.length() - MANIFEST_NAME.length() - 1) {
        return entry;
      }
    }
    // 兼容 manifest.json 直接在 zip 根目录的情况
    return zip.getEntry(MANIFEST_NAME);
  }

  private static void deleteRecursively(Path dir) throws IOException {
    if (!Files.exists(dir)) return;
    try (Stream<Path> walk = Files.walk(dir)) {
      walk.sorted(Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.deleteIfExists(p);
        } catch (IOException ignored) {
          // 清理失败不阻断主流程
        }
      });
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String sanitize(String value) {
    String cleaned = value == null ? "" : value.trim().replaceAll("[^A-Za-z0-9._-]", "-");
    // 整段 "."/".." 会被 normalize 成上级目录，统一替换成 unknown 防越界
    if (cleaned.equals(".") || cleaned.equals("..")) return "unknown";
    return cleaned.isBlank() ? "unknown" : cleaned.toLowerCase(Locale.ROOT);
  }
}
