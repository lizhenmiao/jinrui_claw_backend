package com.jinrui.xlxadmin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinrui.xlxadmin.dto.AdminDtos.VersionSaveRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.UpdateCheckRequest;
import com.jinrui.xlxadmin.dto.ClientDtos.UpdateCheckResult;
import com.jinrui.xlxadmin.entity.LicenseRecord;
import com.jinrui.xlxadmin.entity.VersionInfo;
import com.jinrui.xlxadmin.mapper.VersionMapper;
import com.jinrui.xlxadmin.service.LicenseService;
import com.jinrui.xlxadmin.service.VersionService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VersionServiceImpl extends ServiceImpl<VersionMapper, VersionInfo> implements VersionService {
  private final LicenseService licenseService;
  private final VersionFileStore versionFileStore;
  private final VersionPackageService versionPackageService;

  public VersionServiceImpl(LicenseService licenseService, VersionFileStore versionFileStore,
                            VersionPackageService versionPackageService) {
    this.licenseService = licenseService;
    this.versionFileStore = versionFileStore;
    this.versionPackageService = versionPackageService;
  }

  @Override
  public UpdateCheckResult checkUpdate(UpdateCheckRequest req, HttpServletRequest http) {
    LicenseRecord license = licenseService.requireUsable(req.licenseKey(), req.usbId());
    if (!license.isAllowUpdate()) {
      return new UpdateCheckResult(false, req.currentVersion(), false, "当前授权不允许更新", "", "", List.of(), null, "");
    }
    String channel = blankTo(req.channel(), license.getChannel());
    String platform = blankTo(req.platform(), "windows");
    String arch = blankTo(req.arch(), "x64");
    VersionInfo latest = getOne(Wrappers.lambdaQuery(VersionInfo.class)
        .eq(VersionInfo::getPlatform, platform)
        .eq(VersionInfo::getArch, arch)
        .eq(VersionInfo::getChannel, channel)
        .eq(VersionInfo::isPublished, true)
        .orderByDesc(VersionInfo::getCreatedAt)
        .last("limit 1"));
    if (latest == null) {
      return new UpdateCheckResult(false, req.currentVersion(), false, "暂无更新", "", "", List.of(), null, "");
    }
    boolean newer = compareVersion(latest.getVersion(), req.currentVersion()) > 0;
    // 差量更新：命中新版本、平台在白名单内（如 mac 全量整包不在白名单）且该
    // platform/arch/version 绑定过 manifest 时，下发原文 + 文件下载基础 URL
    // （platform/arch 编进路径）；否则两字段为 null/空，客户端自动回退全量 zip
    boolean deltaEnabled = newer && versionFileStore.isDeltaPlatform(latest.getPlatform());
    String manifest = deltaEnabled
        ? versionFileStore.readManifestRaw(latest.getPlatform(), latest.getArch(), latest.getVersion())
        : null;
    String fileUrlBase = manifest == null ? ""
        : versionPackageService.buildFileUrlBase(http, latest.getPlatform(), latest.getArch());
    return new UpdateCheckResult(
        newer,
        latest.getVersion(),
        latest.isForceUpdate(),
        latest.getTitle(),
        latest.getDownloadUrl(),
        latest.getSha256(),
        splitNotes(latest.getReleaseNotes()),
        manifest,
        fileUrlBase
    );
  }

  @Override
  @Transactional
  public VersionInfo save(Long id, VersionSaveRequest req) {
    VersionInfo item = id == null ? new VersionInfo() : getById(id);
    if (item == null) throw new IllegalArgumentException("版本不存在");
    item.setVersion(blankTo(req.version(), item.getVersion()));
    item.setPlatform(blankTo(req.platform(), "windows"));
    item.setArch(blankTo(req.arch(), "x64"));
    item.setChannel(blankTo(req.channel(), "stable"));
    item.setTitle(req.title());
    item.setReleaseNotes(req.releaseNotes());
    item.setDownloadUrl(req.downloadUrl());
    item.setSha256(req.sha256());
    item.setForceUpdate(Boolean.TRUE.equals(req.forceUpdate()));
    item.setPublished(Boolean.TRUE.equals(req.published()));
    saveOrUpdate(item);
    bindVersionFiles(item);
    return item;
  }

  /**
   * 把上传时暂存的解包文件绑定到 platform/arch/version 三维目录。
   * 暂存不存在（无 manifest 的历史包/纯手填记录）时静默跳过，仅支持全量；
   * 目标目录已存在且内容不同时抛错回滚，避免跨平台同版本号静默覆盖。
   */
  private void bindVersionFiles(VersionInfo item) {
    try {
      versionFileStore.bind(item.getDownloadUrl(), item.getPlatform(), item.getArch(), item.getVersion());
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("绑定版本文件失败：" + e.getMessage(), e);
    }
  }

  private static List<String> splitNotes(String notes) {
    if (notes == null || notes.isBlank()) return List.of();
    return Arrays.stream(notes.split("\\r?\\n")).map(String::trim).filter(s -> !s.isBlank()).toList();
  }

  private static int compareVersion(String a, String b) {
    if (a == null || a.isBlank()) return 0;
    if (b == null || b.isBlank()) return 1;
    String[] aa = a.replaceAll("[^0-9.]", ".").split("\\.");
    String[] bb = b.replaceAll("[^0-9.]", ".").split("\\.");
    int n = Math.max(aa.length, bb.length);
    for (int i = 0; i < n; i++) {
      int ai = i < aa.length && !aa[i].isBlank() ? parseInt(aa[i]) : 0;
      int bi = i < bb.length && !bb[i].isBlank() ? parseInt(bb[i]) : 0;
      if (ai != bi) return Integer.compare(ai, bi);
    }
    return a.compareToIgnoreCase(b);
  }

  private static int parseInt(String value) {
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
      return 0;
    }
  }

  private static String blankTo(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
