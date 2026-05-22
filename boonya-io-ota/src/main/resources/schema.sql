-- 创建数据库
CREATE DATABASE IF NOT EXISTS iot_ota;

\c iot_ota;

-- 固件表
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

CREATE INDEX idx_device_model ON firmware(device_model);
CREATE INDEX idx_status ON firmware(status);

-- OTA任务表
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

CREATE INDEX idx_device_id ON ota_task(device_id);
CREATE INDEX idx_firmware_id ON ota_task(firmware_id);
CREATE INDEX idx_task_status ON ota_task(status);
