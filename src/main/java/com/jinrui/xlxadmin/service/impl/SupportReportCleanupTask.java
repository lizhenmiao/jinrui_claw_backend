package com.jinrui.xlxadmin.service.impl;

import com.jinrui.xlxadmin.service.SupportReportService;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 售后反馈日志包的过期清理任务。
 *
 * 日志包单个可能几十 MB，长期堆积会撑爆磁盘，因此默认保留 90 天：
 * xlx.report-retention-days <= 0 表示保留全部（不自动清理），
 * 执行时间由 xlx.report-cleanup-cron 控制（默认每天 03:10）。
 */
@Component
public class SupportReportCleanupTask {
  private static final Logger log = LoggerFactory.getLogger(SupportReportCleanupTask.class);

  private final SupportReportService supportReportService;
  private final int retentionDays;

  public SupportReportCleanupTask(SupportReportService supportReportService,
                                  @Value("${xlx.report-retention-days:90}") int retentionDays) {
    this.supportReportService = supportReportService;
    this.retentionDays = retentionDays;
  }

  @Scheduled(cron = "${xlx.report-cleanup-cron:0 10 3 * * *}")
  public void purgeExpiredReports() {
    if (retentionDays <= 0) {
      log.info("售后反馈日志包自动清理已关闭（xlx.report-retention-days={}）", retentionDays);
      return;
    }
    LocalDateTime before = LocalDateTime.now().minusDays(retentionDays);
    try {
      int deleted = supportReportService.purgeExpired(before);
      if (deleted > 0) {
        log.info("售后反馈日志包清理完成：删除 {} 条（提交时间早于 {}，保留 {} 天）", deleted, before, retentionDays);
      } else {
        log.debug("售后反馈日志包清理完成：没有早于 {} 的记录", before);
      }
    } catch (RuntimeException e) {
      // 定时任务里吞掉异常，避免一次失败后调度线程被反复刷日志/中断后续执行
      log.warn("售后反馈日志包清理失败（保留 {} 天，截止 {}）：{}", retentionDays, before, e.getMessage(), e);
    }
  }
}
