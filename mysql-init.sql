-- 小龙虾 Java 后台 MySQL 初始化脚本
-- 作用：
-- 1. 创建 xlx_admin 数据库
-- 2. 创建第一版后台需要的核心业务表
-- 3. 给表和字段加中文注释，方便后续维护
--
-- 说明：
-- 当前 Spring Boot 配置里 spring.jpa.hibernate.ddl-auto=update，
-- 所以不手动执行本文件也可以由程序自动建表。
-- 如果你希望数据库结构更清晰、字段有注释，可以先执行本文件。

CREATE DATABASE IF NOT EXISTS xlx_admin
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE xlx_admin;

-- ============================================================
-- 授权表
-- 每卖出一个小龙虾 U 盘，对应一条授权记录。
-- 客户端启动时会带 license_key、usb_id 请求后台校验。
-- 首次使用时，如果 usb_id 为空，可以自动绑定当前 U 盘。
-- ============================================================
CREATE TABLE IF NOT EXISTS licenses (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  license_key VARCHAR(64) NOT NULL COMMENT '授权码，例如 XLX-DEMO-0001',
  customer_name VARCHAR(128) NULL COMMENT '客户名称或备注',
  usb_id VARCHAR(128) NULL COMMENT '绑定的U盘ID，首次绑定后写入',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '授权状态：active正常、disabled禁用、expired过期、revoked吊销',
  expires_at DATETIME NULL COMMENT '授权到期时间，NULL表示不过期',
  allow_update BIT(1) NOT NULL DEFAULT b'1' COMMENT '是否允许该授权检查/下载更新',
  channel VARCHAR(32) NULL DEFAULT 'stable' COMMENT '更新通道：stable稳定版、test测试版、vip高级版',
  model_group VARCHAR(32) NULL DEFAULT 'default' COMMENT '模型分组：default默认、vip高级、internal内部',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  updated_at DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_licenses_license_key (license_key),
  KEY idx_licenses_status (status),
  KEY idx_licenses_usb_id (usb_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小龙虾授权码表';

-- ============================================================
-- 设备表
-- 记录每个授权在哪些电脑上启动过。
-- 后台售后可以通过它看到最近使用时间、系统版本、U盘文件系统等。
-- ============================================================
CREATE TABLE IF NOT EXISTS devices (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  license_id BIGINT NOT NULL COMMENT '关联 licenses.id',
  usb_id VARCHAR(128) NULL COMMENT '客户端上报的U盘ID',
  machine_id VARCHAR(128) NULL COMMENT '客户端生成的机器ID',
  hostname VARCHAR(128) NULL COMMENT '电脑名称',
  os_name VARCHAR(128) NULL COMMENT '操作系统名称，例如 Windows 11',
  arch VARCHAR(32) NULL COMMENT '系统架构，例如 x64、arm64',
  drive_fs VARCHAR(32) NULL COMMENT 'U盘文件系统，例如 NTFS、exFAT、FAT32',
  client_version VARCHAR(64) NULL COMMENT '小龙虾客户端版本号',
  first_seen_at DATETIME NULL COMMENT '首次启动时间',
  last_seen_at DATETIME NULL COMMENT '最近启动/心跳时间',
  launch_count INT NOT NULL DEFAULT 0 COMMENT '累计启动/上报次数',
  last_ip VARCHAR(64) NULL COMMENT '最近一次请求IP',
  PRIMARY KEY (id),
  KEY idx_devices_license_id (license_id),
  KEY idx_devices_license_machine (license_id, machine_id),
  KEY idx_devices_last_seen_at (last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小龙虾设备使用记录表';

-- ============================================================
-- 版本表
-- 后台配置最新版本、下载地址、更新说明。
-- 客户端调用 /api/client/update/check 时，后台从这里找当前通道最新发布版。
-- ============================================================
CREATE TABLE IF NOT EXISTS version_info (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  platform VARCHAR(32) NOT NULL DEFAULT 'windows' COMMENT '平台：windows、macos、linux',
  arch VARCHAR(32) NOT NULL DEFAULT 'x64' COMMENT '架构：x64、arm64、universal',
  version VARCHAR(64) NOT NULL COMMENT '版本号，例如 2026.7.24',
  channel VARCHAR(32) NULL DEFAULT 'stable' COMMENT '版本通道：stable/test/vip',
  title VARCHAR(255) NULL COMMENT '版本标题',
  release_notes TEXT NULL COMMENT '更新说明，一行一条',
  download_url VARCHAR(1024) NULL COMMENT '更新包下载地址',
  sha256 VARCHAR(128) NULL COMMENT '更新包SHA256校验值',
  force_update BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否强制更新',
  published BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否发布，只有发布版本才会下发给客户端',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_version_channel_published_created (channel, published, created_at),
  KEY idx_version_platform_arch (platform, arch, channel, published, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小龙虾版本更新表';

-- ============================================================
-- 模型供应商表
-- 后台管理内置模型配置，例如锦锐内置算力、DeepSeek、通义千问等。
-- 第一版为了简单，将模型列表存在 models_json 字段里。
-- 客户端调用 /api/client/models/config 时，后台按授权的 channel/model_group 下发模型配置。
-- ============================================================
CREATE TABLE IF NOT EXISTS model_provider (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  provider_id VARCHAR(64) NOT NULL COMMENT '模型供应商ID，例如 jinrui-deepseek',
  display_name VARCHAR(128) NOT NULL COMMENT '页面显示名称，例如 锦锐内置算力',
  api_type VARCHAR(64) NOT NULL DEFAULT 'openai-completions' COMMENT '接口类型，例如 openai-completions',
  base_url VARCHAR(1024) NOT NULL COMMENT '模型接口Base URL，例如 http://host:13000/v1',
  api_key_encrypted TEXT NULL COMMENT '加密后的API Key，后台不明文保存',
  key_mode VARCHAR(16) NOT NULL DEFAULT 'client' COMMENT 'Key来源：client=客户端填写，server=后台内置下发',
  models_json TEXT NULL COMMENT '模型列表JSON，例如 deepseek-v4-flash、qwen-max',
  default_model VARCHAR(128) NULL COMMENT '默认模型ID，不包含provider前缀',
  enabled BIT(1) NOT NULL DEFAULT b'1' COMMENT '是否启用该供应商',
  channel VARCHAR(32) NULL DEFAULT 'stable' COMMENT '适用通道：stable/test/vip',
  model_group VARCHAR(32) NULL DEFAULT 'default' COMMENT '适用模型组：default/vip/internal',
  force_default BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否强制客户端使用后台默认模型',
  revision INT NOT NULL DEFAULT 1 COMMENT '模型配置版本号，每次修改递增',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  updated_at DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_model_provider_provider_id (provider_id),
  KEY idx_model_provider_enabled_channel_group (enabled, channel, model_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小龙虾内置模型供应商配置表';

-- ============================================================
-- 客户端事件日志表
-- 用来做售后诊断。
-- 客户端可以上报启动失败、微信二维码超时、QQ离线、模型认证失败等事件。
-- 注意：建议只记录错误摘要，不要上传聊天内容、文件内容、API Key明文。
-- ============================================================
CREATE TABLE IF NOT EXISTS client_event (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  license_id BIGINT NULL COMMENT '关联 licenses.id，可能为空',
  device_id BIGINT NULL COMMENT '关联 devices.id，第一版可为空',
  license_key VARCHAR(64) NULL COMMENT '客户端上报的授权码',
  event_type VARCHAR(64) NULL COMMENT '事件类型，例如 startup_failed、wechat_qr_timeout',
  level VARCHAR(32) NULL COMMENT '事件级别：info、warn、error',
  message VARCHAR(1024) NULL COMMENT '事件摘要',
  details_json TEXT NULL COMMENT '事件详情JSON，不应包含隐私内容或密钥明文',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_client_event_license_id (license_id),
  KEY idx_client_event_license_key (license_key),
  KEY idx_client_event_level_created (level, created_at),
  KEY idx_client_event_type_created (event_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小龙虾客户端事件日志表';

-- Optional: for production, create a dedicated MySQL user instead of root.
-- CREATE USER IF NOT EXISTS 'xlx'@'localhost' IDENTIFIED BY 'change-this-password';
-- GRANT ALL PRIVILEGES ON xlx_admin.* TO 'xlx'@'localhost';
-- FLUSH PRIVILEGES;
