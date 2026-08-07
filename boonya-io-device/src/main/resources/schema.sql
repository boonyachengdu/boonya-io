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

CREATE INDEX idx_device_id ON device(device_id);
CREATE INDEX idx_status ON device(status);
CREATE INDEX idx_group_id ON device(group_id);
CREATE INDEX idx_tenant_id ON device(tenant_id);

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

CREATE INDEX idx_log_device_id ON device_log(device_id);
CREATE INDEX idx_log_create_time ON device_log(create_time);

-- 告警表
CREATE TABLE IF NOT EXISTS device_alert (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(64) NOT NULL,
    alert_type VARCHAR(32) NOT NULL,
    severity VARCHAR(16) NOT NULL DEFAULT 'WARNING',
    title VARCHAR(256) NOT NULL,
    message TEXT,
    metric_value DOUBLE PRECISION,
    threshold DOUBLE PRECISION,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    trigger_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ack_time TIMESTAMP,
    resolve_time TIMESTAMP,
    operator VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_alert_device_id ON device_alert(device_id);
CREATE INDEX IF NOT EXISTS idx_alert_status ON device_alert(status);
CREATE INDEX IF NOT EXISTS idx_alert_severity ON device_alert(severity);
CREATE INDEX IF NOT EXISTS idx_alert_trigger_time ON device_alert(trigger_time);
