-- 用户表
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

CREATE INDEX idx_username ON sys_user(username);
CREATE INDEX idx_status ON sys_user(status);

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(64) NOT NULL,
    role_code VARCHAR(64) UNIQUE NOT NULL,
    description TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, role_id)
);

CREATE INDEX idx_user_id ON sys_user_role(user_id);
CREATE INDEX idx_role_id ON sys_user_role(role_id);

-- 插入默认管理员用户 (密码: admin123)
INSERT INTO sys_user (username, password, email, real_name, status)
VALUES ('admin', '$2a$10$T5otds6e2hRTufPuxxzH..Jc430PiukykE0Nqv0uCmdFfm1NUqc1K', 'admin@example.com', '系统管理员', 'active')
ON CONFLICT (username) DO NOTHING;

-- 插入默认角色
INSERT INTO sys_role (role_name, role_code, description)
VALUES
    ('超级管理员', 'ROLE_ADMIN', '拥有所有权限'),
    ('普通用户', 'ROLE_USER', '基本权限')
ON CONFLICT (role_code) DO NOTHING;

-- 给 admin 用户分配 ROLE_ADMIN 角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.role_code = 'ROLE_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) UNIQUE NOT NULL,
    type VARCHAR(16) NOT NULL DEFAULT 'MENU',
    path VARCHAR(128),
    component VARCHAR(128),
    icon VARCHAR(32),
    sort INTEGER DEFAULT 0,
    enabled INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);

CREATE INDEX idx_role_permission_role_id ON sys_role_permission(role_id);
CREATE INDEX idx_permission_parent_id ON sys_permission(parent_id);

-- 插入默认菜单权限
INSERT INTO sys_permission (parent_id, name, code, type, path, icon, sort) VALUES
    (0, '数据看板', 'dashboard', 'MENU', '/dashboard', 'DataAnalysis', 1),
    (0, '设备管理', 'device', 'MENU', '/devices', 'Monitor', 2),
    (0, '固件管理', 'firmware', 'MENU', '/firmware', 'Upload', 3),
    (0, 'OTA任务', 'ota', 'MENU', '/ota-tasks', 'Refresh', 4),
    (0, '数据分析', 'analytics', 'MENU', '/analytics', 'TrendCharts', 5),
    (0, '告警管理', 'alert', 'MENU', '/alerts', 'Bell', 6),
    (0, '告警规则', 'alert_rule', 'MENU', '/alert-rules', 'Setting', 7),
    (0, '能碳管理', 'energy', 'MENU', '/energy', 'Sunny', 8),
    (0, '系统管理', 'system', 'MENU', '/system', 'Tools', 9),
    (9, '用户管理', 'system:user', 'MENU', '/system/users', 'UserFilled', 1),
    (9, '角色管理', 'system:role', 'MENU', '/system/roles', 'UserFilled', 2),
    (0, 'AI分析', 'ai', 'MENU', '/ai-analysis', 'Cpu', 10)
ON CONFLICT (code) DO NOTHING;

-- 给 ROLE_ADMIN 分配所有权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'ROLE_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;
