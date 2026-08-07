#!/bin/bash
set -e
# 统一初始化所有业务数据库与表结构（Postgres 容器首次启动时执行一次）
# iot_device 由 POSTGRES_DB 环境变量自动创建，这里补建 iot_auth / iot_ota 并统一建表

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE iot_auth;
    CREATE DATABASE iot_ota;
EOSQL

# ========== iot_device 表结构 ==========
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "iot_device" <<-EOSQL
CREATE TABLE IF NOT EXISTS device (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(64) UNIQUE NOT NULL,
    device_name VARCHAR(128) NOT NULL,
    device_type VARCHAR(64),
    model VARCHAR(64),
    firmware_version VARCHAR(32),
    status VARCHAR(16) DEFAULT 'inactive',
    last_heartbeat TIMESTAMP,
    group_id BIGINT,
    tenant_id BIGINT DEFAULT 0,
    location VARCHAR(256),
    description TEXT,
    auth_token VARCHAR(128),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_device_id ON device(device_id);
CREATE INDEX IF NOT EXISTS idx_device_status ON device(status);
CREATE INDEX IF NOT EXISTS idx_device_group_id ON device(group_id);
CREATE INDEX IF NOT EXISTS idx_device_tenant_id ON device(tenant_id);

CREATE TABLE IF NOT EXISTS device_group (
    id BIGSERIAL PRIMARY KEY,
    group_name VARCHAR(128) NOT NULL,
    group_code VARCHAR(64) UNIQUE NOT NULL,
    parent_id BIGINT DEFAULT 0,
    description TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS device_log (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(64) NOT NULL,
    log_type VARCHAR(16) NOT NULL,
    message TEXT,
    detail JSONB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_device_log_device_id ON device_log(device_id);
CREATE INDEX IF NOT EXISTS idx_device_log_create_time ON device_log(create_time);

-- 设备表添加 product_key 列（兼容已有数据）
ALTER TABLE device ADD COLUMN IF NOT EXISTS product_key VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_device_product_key ON device(product_key);

-- 产品（设备模板）
CREATE TABLE IF NOT EXISTS iot_product (
    id BIGSERIAL PRIMARY KEY,
    product_key VARCHAR(64) UNIQUE NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    node_type VARCHAR(16) DEFAULT 'DIRECT',
    protocol_type VARCHAR(16) DEFAULT 'MQTT',
    data_format VARCHAR(16) DEFAULT 'JSON',
    description TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_iot_product_key ON iot_product(product_key);

-- 物模型属性
CREATE TABLE IF NOT EXISTS iot_thing_model (
    id BIGSERIAL PRIMARY KEY,
    product_key VARCHAR(64) NOT NULL,
    identifier VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    data_type VARCHAR(16) NOT NULL,
    unit VARCHAR(32),
    min_value DOUBLE PRECISION,
    max_value DOUBLE PRECISION,
    access_mode VARCHAR(16) DEFAULT 'RW',
    description TEXT,
    sort INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_key, identifier)
);
CREATE INDEX IF NOT EXISTS idx_iot_thing_model_product_key ON iot_thing_model(product_key);

-- 物模型服务
CREATE TABLE IF NOT EXISTS iot_thing_service (
    id BIGSERIAL PRIMARY KEY,
    product_key VARCHAR(64) NOT NULL,
    identifier VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    call_type VARCHAR(16) DEFAULT 'ASYNC',
    input_params TEXT,
    output_params TEXT,
    description TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_key, identifier)
);
CREATE INDEX IF NOT EXISTS idx_iot_thing_service_product_key ON iot_thing_service(product_key);
EOSQL

# ========== iot_auth 表结构 ==========
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "iot_auth" <<-EOSQL
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(128),
    phone VARCHAR(20),
    real_name VARCHAR(64),
    status VARCHAR(16) DEFAULT 'active',
    last_login_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_sys_user_username ON sys_user(username);
CREATE INDEX IF NOT EXISTS idx_sys_user_status ON sys_user(status);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(64) NOT NULL,
    role_code VARCHAR(64) UNIQUE NOT NULL,
    description TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, role_id)
);
CREATE INDEX IF NOT EXISTS idx_sys_user_role_user_id ON sys_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_role_role_id ON sys_user_role(role_id);

-- 默认管理员 (密码: admin123)
INSERT INTO sys_user (username, password, email, real_name, status)
VALUES ('admin', '$2a$10$T5otds6e2hRTufPuxxzH..Jc430PiukykE0Nqv0uCmdFfm1NUqc1K', 'admin@example.com', '系统管理员', 'active')
ON CONFLICT (username) DO NOTHING;

INSERT INTO sys_role (role_name, role_code, description) VALUES
    ('超级管理员', 'ROLE_ADMIN', '拥有所有权限'),
    ('普通用户', 'ROLE_USER', '基本权限')
ON CONFLICT (role_code) DO NOTHING;
EOSQL

# ========== iot_ota 表结构 ==========
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "iot_ota" <<-EOSQL
CREATE TABLE IF NOT EXISTS firmware (
    id BIGSERIAL PRIMARY KEY,
    device_model VARCHAR(64) NOT NULL,
    version VARCHAR(32) NOT NULL,
    description TEXT,
    file_path VARCHAR(512),
    file_name VARCHAR(256),
    file_size BIGINT,
    md5_checksum VARCHAR(64),
    status VARCHAR(16) DEFAULT 'draft',
    force_update BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    publish_time TIMESTAMP,
    UNIQUE (device_model, version)
);
CREATE INDEX IF NOT EXISTS idx_firmware_device_model ON firmware(device_model);
CREATE INDEX IF NOT EXISTS idx_firmware_status ON firmware(status);

CREATE TABLE IF NOT EXISTS ota_task (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(64) NOT NULL,
    firmware_id BIGINT NOT NULL,
    status VARCHAR(32) DEFAULT 'pending',
    error_message TEXT,
    progress INTEGER DEFAULT 0,
    start_time TIMESTAMP,
    complete_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (firmware_id) REFERENCES firmware(id)
);
CREATE INDEX IF NOT EXISTS idx_ota_task_device_id ON ota_task(device_id);
CREATE INDEX IF NOT EXISTS idx_ota_task_firmware_id ON ota_task(firmware_id);
CREATE INDEX IF NOT EXISTS idx_ota_task_status ON ota_task(status);
EOSQL
