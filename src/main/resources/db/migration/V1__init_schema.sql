create table if not exists licenses (
  id bigint not null auto_increment comment 'Primary key',
  license_key varchar(64) not null comment 'License key',
  customer_name varchar(128) null comment 'Customer name or note',
  usb_id varchar(128) null comment 'Bound USB id',
  status varchar(32) not null default 'active' comment 'active/disabled/expired/revoked',
  expires_at datetime null comment 'Expiration time',
  allow_update bit(1) not null default b'1' comment 'Allow client update',
  channel varchar(32) null default 'stable' comment 'Update channel',
  model_group varchar(32) null default 'default' comment 'Model group',
  created_at datetime not null comment 'Created time',
  updated_at datetime not null comment 'Updated time',
  primary key (id),
  unique key uk_licenses_license_key (license_key),
  key idx_licenses_status (status),
  key idx_licenses_usb_id (usb_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='License records';

create table if not exists devices (
  id bigint not null auto_increment comment 'Primary key',
  license_id bigint not null comment 'licenses.id',
  usb_id varchar(128) null comment 'Client USB id',
  machine_id varchar(128) null comment 'Client machine id',
  hostname varchar(128) null comment 'Host name',
  os_name varchar(128) null comment 'Operating system',
  arch varchar(32) null comment 'CPU architecture',
  drive_fs varchar(32) null comment 'USB file system',
  client_version varchar(64) null comment 'Client version',
  first_seen_at datetime null comment 'First seen time',
  last_seen_at datetime null comment 'Last seen time',
  launch_count int not null default 0 comment 'Launch/report count',
  last_ip varchar(64) null comment 'Last request IP',
  primary key (id),
  key idx_devices_license_id (license_id),
  key idx_devices_license_machine (license_id, machine_id),
  key idx_devices_last_seen_at (last_seen_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='Device records';

create table if not exists version_info (
  id bigint not null auto_increment comment 'Primary key',
  version varchar(64) not null comment 'Version number',
  platform varchar(32) not null default 'windows' comment 'windows/macos',
  arch varchar(32) not null default 'x64' comment 'x64/arm64/universal',
  channel varchar(32) null default 'stable' comment 'Release channel',
  title varchar(255) null comment 'Update title',
  release_notes text null comment 'Release notes',
  download_url varchar(1024) null comment 'Package download URL',
  sha256 varchar(128) null comment 'Package SHA256',
  force_update bit(1) not null default b'0' comment 'Force update',
  published bit(1) not null default b'0' comment 'Published',
  created_at datetime not null comment 'Created time',
  primary key (id),
  key idx_version_channel_published_created (channel, published, created_at),
  key idx_version_platform_arch (platform, arch, channel, published, created_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='Version update records';

create table if not exists model_provider (
  id bigint not null auto_increment comment 'Primary key',
  provider_id varchar(64) not null comment 'Provider id',
  display_name varchar(128) not null comment 'Display name',
  api_type varchar(64) not null default 'openai-completions' comment 'API type',
  base_url varchar(1024) not null comment 'Base URL',
  api_key_encrypted text null comment 'Encrypted API key',
  key_mode varchar(16) not null default 'client' comment 'client/server',
  models_json text null comment 'Models JSON',
  default_model varchar(128) null comment 'Default model id',
  enabled bit(1) not null default b'1' comment 'Enabled',
  channel varchar(32) null default 'stable' comment 'Channel',
  model_group varchar(32) null default 'default' comment 'Model group',
  force_default bit(1) not null default b'0' comment 'Force default model',
  revision int not null default 1 comment 'Config revision',
  created_at datetime not null comment 'Created time',
  updated_at datetime not null comment 'Updated time',
  primary key (id),
  unique key uk_model_provider_provider_id (provider_id),
  key idx_model_provider_enabled_channel_group (enabled, channel, model_group)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='Model provider config';

create table if not exists client_event (
  id bigint not null auto_increment comment 'Primary key',
  license_id bigint null comment 'licenses.id',
  device_id bigint null comment 'devices.id',
  license_key varchar(64) null comment 'Client license key',
  event_type varchar(64) null comment 'Event type',
  level varchar(32) null comment 'info/warn/error',
  message varchar(1024) null comment 'Message summary',
  details_json text null comment 'Event details JSON',
  created_at datetime not null comment 'Created time',
  primary key (id),
  key idx_client_event_license_id (license_id),
  key idx_client_event_license_key (license_key),
  key idx_client_event_level_created (level, created_at),
  key idx_client_event_type_created (event_type, created_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='Client event logs';
