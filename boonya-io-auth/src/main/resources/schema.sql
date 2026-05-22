-- 创建数据库
CREATE DATABASE IF NOT EXISTS iot_auth;

\c iot_auth;

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
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@example.com', '系统管理员', 'active')
ON CONFLICT (username) DO NOTHING;

-- 插入默认角色
INSERT INTO sys_role (role_name, role_code, description)
VALUES 
    ('超级管理员', 'ROLE_ADMIN', '拥有所有权限'),
    ('普通用户', 'ROLE_USER', '基本权限')
ON CONFLICT (role_code) DO NOTHING;
