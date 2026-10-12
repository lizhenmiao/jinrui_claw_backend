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
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VersionServiceImpl extends ServiceImpl<VersionMapper, VersionInfo> implements VersionService {
  private final LicenseService licenseService;

  public VersionServiceImpl(LicenseService licenseService) {
    this.licenseService = licenseService;
  }

  @Override
  public UpdateCheckResult checkUpdate(UpdateCheckRequest req) {
    LicenseRecord license = licenseService.requireUsable(req.licenseKey(), req.usbId());
    if (!license.isAllowUpdate()) {
      return new UpdateCheckResult(false, req.currentVersion(), false, "当前授权不允许更新", "", "", List.of());
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
      return new UpdateCheckResult(false, req.currentVersion(), false, "暂无更新", "", "", List.of());
    }
    boolean newer = compareVersion(latest.getVersion(), req.currentVersion()) > 0;
    return new UpdateCheckResult(
        newer,
        latest.getVersion(),
        latest.isForceUpdate(),
        latest.getTitle(),
        latest.getDownloadUrl(),
        latest.getSha256(),
        splitNotes(latest.getReleaseNotes())
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
    return item;
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
