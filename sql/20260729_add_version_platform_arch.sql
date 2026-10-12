ALTER TABLE version_info
  ADD COLUMN platform VARCHAR(32) NOT NULL DEFAULT 'windows' COMMENT 'platform: windows, macos, linux',
  ADD COLUMN arch VARCHAR(32) NOT NULL DEFAULT 'x64' COMMENT 'arch: x64, arm64, universal';

CREATE INDEX idx_version_platform_arch
ON version_info(platform, arch, channel, published, created_at);
