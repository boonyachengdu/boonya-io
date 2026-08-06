# Boonya IoT 平台 - 项目进展

## 📊 当前状态：8/8 后端模块 + 前端全部完成

### ✅ 后端模块

| 模块 | 端口   | 状态 | 说明 |
|------|------|------|------|
| boonya-io-common | -    | ✅ | 公共模块（异常、响应、工具类） |
| boonya-io-gateway | 8080 | ✅ | API 网关（路由、鉴权） |
| boonya-io-iot | 8081 | ✅ | IoT 核心（MQTT、规则引擎、告警） |
| boonya-io-minio | 8082 | ✅ | 对象存储（文件上传下载） |
| boonya-io-auth | 8083 | ✅ | 认证授权（JWT、登录注册） |
| boonya-io-analytics | 8084 | ✅ | 数据分析（实时数据、趋势） |
| boonya-io-ota | 8085 | ✅ | OTA 升级（固件管理、任务调度） |
| boonya-io-device | 8086 | ✅ | 设备管理（注册、心跳、状态） |

### ✅ 前端模块

| 模块 | 技术栈 | 状态 | 说明 |
|------|--------|------|------|
| boonya-io-frontend/admin | Vue 3 + TypeScript | ✅ | 管理后台 |
| boonya-io-frontend/h5 | Vue 3 + TypeScript | ✅ | 移动端 H5 |

---

## 🏗️ 项目架构

```
boonya-io/
├── boonya-io-common/          # ✅ 公共模块
├── boonya-io-auth/            # ✅ 认证授权 (Port: 8083)
├── boonya-io-device/          # ✅ 设备管理 (Port: 8086)
├── boonya-io-iot/             # ✅ IoT 核心 (Port: 8081)
├── boonya-io-analytics/       # ✅ 数据分析 (Port: 8084)
├── boonya-io-minio/           # ✅ 对象存储 (Port: 8082)
├── boonya-io-gateway/         # ✅ API 网关 (Port: 8080)
├── boonya-io-ota/             # ✅ OTA 升级 (Port: 8085)
├── boonya-io-frontend/        # ✅ 前端 (admin + h5)
│   ├── admin/                 # 管理后台
│   └── h5/                    # 移动端
├── data/                      # 数据目录
└── docker-compose.yml         # 一键部署编排
```

---

## 🔧 技术栈总览

| 类别 | 技术 | 版本 |
|------|------|------|
| **基础框架** | Spring Boot | 3.3.5 |
| **微服务** | Spring Cloud Gateway | 2023.0.3 |
| **数据库** | PostgreSQL | 15 |
| **时序数据库** | TDengine | Latest |
| **缓存** | Redis | 7 |
| **ORM** | MyBatis-Plus / Spring Data JPA | 3.5.9 |
| **MQTT** | EMQX / Moquette | 5.4 |
| **对象存储** | MinIO | Latest |
| **API 文档** | Swagger + Knife4j | 4.5.0 |
| **前端** | Vue 3 + TypeScript | - |
| **Java** | JDK | 17 |

---

## 🚀 快速启动

### 前置要求
- JDK 17+
- Maven 3.8+
- Docker & Docker Compose

### 一键启动
```bash
cd D:\code\boonya-io

# 代码有变动一定要--build（先本地mvn package构建JAR）
# 第一步：Maven构建
mvn clean package -DskipTests

# 第二步：Docker镜像+容器（有代码变动/首次执行必须带--build）
docker-compose up -d --build

# 仅启动（代码/镜像均无变动时使用）
docker-compose up -d

# 查看运行状态
docker-compose ps
```

这将启动：PostgreSQL、TDengine、Redis、EMQX、MinIO、Gateway、Auth、Device、IoT、Analytics、OTA

### 本地开发启动
```bash
# 1. 安装公共模块
mvn clean install -pl boonya-io-common

# 2. 按顺序启动各服务
cd boonya-io-auth && mvn spring-boot:run
cd boonya-io-device && mvn spring-boot:run
cd boonya-io-iot && mvn spring-boot:run
cd boonya-io-analytics && mvn spring-boot:run
cd boonya-io-ota && mvn spring-boot:run
cd boonya-io-minio && mvn spring-boot:run
cd boonya-io-gateway && mvn spring-boot:run
```

---

## 📖 文档

- [架构设计文档](./ARCHITECTURE.md)
- [模块实现清单](./MODULES_CHECKLIST.md)
- [完成总结](./COMPLETION_SUMMARY.md)
- [最终报告](./FINAL_REPORT.md)

---

**最后更新**: 2026-05-22
**维护者**: Boonya Lab Team
