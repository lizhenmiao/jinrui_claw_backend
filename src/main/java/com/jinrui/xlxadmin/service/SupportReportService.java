package com.jinrui.xlxadmin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jinrui.xlxadmin.dto.ClientDtos.SupportReportMeta;
import com.jinrui.xlxadmin.dto.ClientDtos.SupportReportResult;
import com.jinrui.xlxadmin.entity.SupportReport;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.springframework.web.multipart.MultipartFile;

public interface SupportReportService extends IService<SupportReport> {
  /**
   * 接收客户端「一键提交日志」—— 宽松校验（不依赖授权可用状态），
   * 用 HMAC 签名 + 频率限流 + reportNo 幂等兜底。
   */
  SupportReportResult submit(SupportReportMeta meta, MultipartFile file, HttpServletRequest http);

  /** 定位反馈的日志包文件；不存在返回 null。 */
  Path filePath(SupportReport report);

  /** 后台更新反馈状态/备注；status 为 null/空时只更新备注。 */
  SupportReport updateStatus(Long id, String status, String adminNote);

  /**
   * 清理 before 之前提交的反馈：先删记录再尽力删日志包文件，分批处理避免大事务。
   * 返回删除的记录数。
   */
  int purgeExpired(LocalDateTime before);
}