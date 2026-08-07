-- PostgreSQL 表结构初始化脚本（iot 模块，连接 iot_device 库）
-- 由 PgJdbcConfig 在启动时自动执行（IF NOT EXISTS，幂等）
-- 与 TDengine 时序数据存储相互独立

-- 告警记录表：AlertHandler 触发告警时写入
CREATE TABLE IF NOT EXISTS device_alert (
    id            BIGSERIAL PRIMARY KEY,
    device_id     VARCHAR(64)  NOT NULL,
    alert_type    VARCHAR(32)  NOT NULL,          -- 告警类型，如 OVER_TEMP
    severity      VARCHAR(16)  NOT NULL DEFAULT 'WARNING', -- INFO / WARNING / CRITICAL
    title         VARCHAR(128),
    message       TEXT,
    metric_value  DOUBLE PRECISION,               -- 触发时的实际指标值
    threshold     DOUBLE PRECISION,               -- 阈值（若事件未携带则为 NULL）
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING', -- PENDING / ACK / RESOLVED
    trigger_time  TIMESTAMP    NOT NULL DEFAULT NOW(),
    create_time   TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_device_alert_device     ON device_alert (device_id);
CREATE INDEX IF NOT EXISTS idx_device_alert_status     ON device_alert (status);
CREATE INDEX IF NOT EXISTS idx_device_alert_trigger_time ON device_alert (trigger_time);

-- 告警规则表：RuleEngine 启动时加载到内存，CRUD 同步写回
CREATE TABLE IF NOT EXISTS alert_rule (
    id            BIGSERIAL PRIMARY KEY,
    rule_id       VARCHAR(64)  NOT NULL UNIQUE,    -- 业务规则标识（UUID 或 temp_high_alert 等）
    rule_name     VARCHAR(128),
    device_id     VARCHAR(64),                     -- 设备 ID（NULL 表示全局规则）
    metric        VARCHAR(32),                     -- 指标名：temp / humidity 等
    operator      VARCHAR(8),                      -- 运算符: > / < / >= / <= / == / !=
    threshold     DOUBLE PRECISION,
    logic         VARCHAR(8),                      -- 多条件逻辑 AND/OR（单条件为 NULL）
    action        VARCHAR(16),                     -- 动作：ALERT / FORWARD / STORE
    severity      VARCHAR(16)  DEFAULT 'WARNING',  -- INFO / WARNING / CRITICAL
    cooldown_ms   BIGINT       DEFAULT 0,          -- 冷却时间（毫秒）
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    create_time   TIMESTAMP    NOT NULL DEFAULT NOW(),
    update_time   TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_alert_rule_device ON alert_rule (device_id);
CREATE INDEX IF NOT EXISTS idx_alert_rule_enabled ON alert_rule (enabled);
