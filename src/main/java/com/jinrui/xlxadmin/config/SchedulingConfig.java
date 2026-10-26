package com.jinrui.xlxadmin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务开关 —— 目前只有售后反馈日志包的过期清理任务（见 SupportReportCleanupTask）。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}