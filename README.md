# Boonya IoT 物联网平台

## 📖 项目简介

**Boonya IoT** 是一个企业级的物联网（IoT）微服务平台，提供完整的设备全生命周期管理、数据采集传输、实时监控告警、OTA 固件升级、数据分析可视化等能力。平台采用 Spring Cloud 微服务架构，支持高可用部署和水平扩展。

### ✨ 核心特性

- **微服务架构**：基于 Spring Boot 3 + Spring Cloud 的模块化设计
- **设备接入**：支持 MQTT 协议，兼容 EMQX 和嵌入式 Moquette Broker
- **时序数据**：TDengine 高性能时序数据库存储设备遥测数据
- **实时监控**：WebSocket 实时推送设备数据和告警信息
- **OTA 升级**：完整的固件版本管理和远程升级调度
- **数据分析**：多维度统计分析和可视化数据看板
- **对象存储**：MinIO 集成，支持固件、日志、媒体文件存储
- **AI 集成**：Spring AI + Spring AI Alibaba 支持智能分析能力
- **权限管理**：JWT 无状态认证 + RBAC 细粒度权限控制
- **容器化部署**：Docker Compose 一键启动完整环境

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                      客户端层 (Clients)                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ Admin 后台│  │  H5 移动端│  │ 第三方API│  │ 设备端   │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/WebSocket/MQTT
┌────────────────────────▼────────────────────────────────────┐
│                   API 网关层 (Gateway :8080)                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Spring Cloud Gateway - 路由/鉴权/限流/负载均衡       │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                    微服务层 (Microservices)                   │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Auth Service │  │Device Service│  │ IoT Core     │      │
│  │  (:8083)     │  │  (:8086)     │  │  (:8081)     │      │
│  │ JWT/RBAC     │  │ 设备CRUD     │  │ MQTT接入      │      │
│  │ 用户管理     │  │ 状态监控     │  │ 规则引擎      │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │Analytics Svc │  │ MinIO Svc    │  │ OTA Service  │      │
│  │  (:8084)     │  │  (:8082)     │  │  (:8085)     │      │
│  │ 统计分析     │  │ 文件存储     │  │ 固件管理     │      │
│  │ 数据看板     │  │ 日志归档     │  │ 升级调度     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                  数据存储层 (Data Storage)                    │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │PostgreSQL│  │ TDengine │  │  Redis   │  │  MinIO   │   │
│  │ 业务数据  │  │ 时序数据  │  │ 缓存会话  │  │ 对象存储  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.3.5 | 应用框架 |
| Spring Cloud | 2023.0.3 | 微服务框架 |
| Spring Cloud Alibaba | 2023.0.1.0 | 微服务生态（Nacos等） |
| Spring AI | 1.0.0-M6 | AI 集成框架 |
| Spring AI Alibaba | 1.0.0-M6.1 | 阿里云 AI 集成 |
| MyBatis-Plus | 3.5.9 | ORM 持久层框架 |
| Knife4j + SpringDoc | 4.5.0 + 2.6.0 | API 文档 |
| Lombok | - | 简化代码 |

### 数据存储与中间件

| 技术 | 版本 | 端口 | 说明 |
|------|------|------|------|
| PostgreSQL | 15-alpine | 5432 | 业务关系型数据库 |
| TDengine | latest | 6030/6041 | 时序数据库 |
| Redis | 7-alpine | 6379 | 缓存、会话存储 |
| EMQX | 5.4 | 1883/18083 | MQTT 消息 Broker |
| MinIO | latest | 9000/9001 | 对象存储 |

### 前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4 | 前端框架 |
| TypeScript | 5.3 | 类型系统 |
| Vite | 5.0 | 构建工具 |
| Element Plus | 2.5 | Admin 后台 UI |
| Vant | 4.8 | H5 移动端 UI |
| Pinia | 2.1 | 状态管理 |
| Vue Router | 4.2 | 路由管理 |
| Axios | 1.6 | HTTP 客户端 |
| ECharts | 5.4 | 数据可视化图表 |
| MQTT.js | 5.3 | MQTT 客户端 |

---

## 📁 项目结构

```
boonya-io/
├── boonya-io-common/          # 公共模块
│   └── com.boonya.lab.io.common
│       ├── config/            # 全局异常处理配置
│       ├── constant/          # 常量定义
│       ├── exception/         # 自定义异常体系
│       ├── response/          # 统一响应封装 Result/PageResult
│       └── util/              # JSON/Token 工具类
│
├── boonya-io-gateway/         # API 网关服务 (8080)
│   └── Spring Cloud Gateway - 路由转发、统一鉴权、限流熔断
│
├── boonya-io-auth/            # 认证授权服务 (8083)
│   ├── controller/AuthController    # 登录/刷新/登出接口
│   ├── service/AuthService          # JWT 认证业务逻辑
│   ├── util/JwtUtils                # JWT 生成验证工具
│   ├── entity/User                  # 用户实体
│   └── schema.sql                   # 用户/角色表结构
│
├── boonya-io-device/          # 设备管理服务 (8086)
│   ├── controller/DeviceController  # 设备注册/查询/心跳接口
│   ├── service/DeviceService        # 设备生命周期管理
│   ├── entity/                      # Device/DeviceGroup/DeviceLog
│   ├── dto/                         # 请求响应 DTO
│   └── schema.sql                   # 设备相关表结构
│
├── boonya-io-iot/             # IoT 核心服务 (8081)
│   ├── mqtt/                        # MQTT 客户端封装（EMQX/嵌入式）
│   ├── service/MqttSubscriber       # MQTT 消息订阅处理
│   ├── service/TimeSeriesService    # TDengine 时序数据操作
│   ├── ruleengine/                  # 规则引擎（条件判断）
│   ├── event/handler/AlertHandler   # 告警事件处理
│   ├── device/DeviceSimulator       # 虚拟设备模拟器（100台）
│   ├── config/WebSocketConfig       # WebSocket 实时推送
│   └── controller/                  # 设备数据/规则/健康接口
│
├── boonya-io-minio/           # 对象存储服务 (8082)
│   └── MinIO 文件上传下载、预签名URL、Bucket 管理
│
├── boonya-io-analytics/       # 数据分析服务 (8084)
│   └── 实时看板、历史查询、统计聚合、趋势分析、报表导出
│
├── boonya-io-ota/             # OTA 升级服务 (8085)
│   ├── 固件管理（上传/版本/发布/归档）
│   ├── 升级任务（创建/进度跟踪/取消）
│   └── schema.sql                   # 固件/OTA任务表结构
│
├── boonya-io-cache/           # 缓存模块
│   └── CacheType 缓存类型定义
│
├── boonya-io-frontend/        # 前端项目
│   ├── admin/                 # 管理后台（Vue 3 + Element Plus）
│   │   ├── views/
│   │   │   ├── Login.vue            # 登录页
│   │   │   ├── Dashboard.vue        # 数据看板（ECharts图表）
│   │   │   ├── devices/DeviceList.vue   # 设备列表管理
│   │   │   ├── ota/FirmwareList.vue     # 固件管理
│   │   │   ├── ota/OtaTaskList.vue      # OTA任务列表
│   │   │   └── analytics/DataAnalytics.vue  # 数据分析
│   │   ├── api/                   # auth/device API 封装
│   │   ├── stores/user.ts         # Pinia 用户状态
│   │   ├── router/index.ts        # 路由配置（含守卫）
│   │   └── layouts/MainLayout.vue # 主布局（侧边栏+顶栏）
│   │
│   └── h5/                    # H5 移动端（Vue 3 + Vant）
│       └── 设备监控、实时数据、告警通知
│
├── docker-compose.yml         # 完整容器化编排
├── pom.xml                    # Maven 父工程配置
└── ARCHITECTURE.md            # 详细架构设计文档
```

---

## 🚀 快速开始

### 环境要求

- **JDK**: 17+
- **Maven**: 3.8+
- **Node.js**: 18+
- **Docker**: 20.10+ & Docker Compose

### 方式一：Docker Compose 一键启动（推荐）

```bash
# 1. 克隆项目并进入根目录
cd boonya-io

# 2. 启动所有服务（基础组件 + 微服务）
docker-compose up -d

# 3. 查看服务状态
docker-compose ps
```

**启动后访问地址**：

| 服务 | 地址 | 默认账号 |
|------|------|----------|
| API 网关 | http://localhost:8080 | - |
| Swagger API 文档 | http://localhost:8080/swagger-ui.html | - |
| Admin 管理后台 | http://localhost:3000 | admin / admin123 |
| EMQX 控制台 | http://localhost:18083 | admin / public |
| MinIO 控制台 | http://localhost:9001 | minioadmin / minioadmin |

### 方式二：本地开发启动

#### 1. 启动基础设施（Docker）

```bash
# 仅启动数据库、中间件
docker-compose up -d postgres tdengine redis emqx minio
```

#### 2. 初始化数据库

```bash
# 认证数据库
psql -U postgres -h localhost -f boonya-io-auth/src/main/resources/schema.sql

# 设备数据库
psql -U postgres -h localhost -f boonya-io-device/src/main/resources/schema.sql

# OTA数据库
psql -U postgres -h localhost -f boonya-io-ota/src/main/resources/schema.sql
```

#### 3. 启动后端微服务

按依赖顺序启动各模块：

```bash
# 1. 公共模块（先安装到本地仓库）
cd boonya-io-common
mvn clean install

# 2. 网关服务 (8080)
cd ../boonya-io-gateway
mvn spring-boot:run

# 3. 认证服务 (8083)
cd ../boonya-io-auth
mvn spring-boot:run

# 4. 设备管理服务 (8086)
cd ../boonya-io-device
mvn spring-boot:run

# 5. IoT核心服务 (8081)
cd ../boonya-io-iot
mvn spring-boot:run

# 6. 对象存储服务 (8082)
cd ../boonya-io-minio
mvn spring-boot:run

# 7. 数据分析服务 (8084)
cd ../boonya-io-analytics
mvn spring-boot:run

# 8. OTA升级服务 (8085)
cd ../boonya-io-ota
mvn spring-boot:run
```

#### 4. 启动前端项目

```bash
# Admin 后台
cd boonya-io-frontend/admin
npm install
npm run dev          # http://localhost:3000

# H5 移动端（另一个终端）
cd boonya-io-frontend/h5
npm install
npm run dev          # http://localhost:3001
```

---

## 📡 API 概览

所有 API 通过网关统一入口：`http://localhost:8080/api/...`

### 认证模块 (`/api/auth/**`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录，返回 JWT Token |
| POST | `/api/auth/refresh` | 刷新 Access Token |
| POST | `/api/auth/logout` | 登出（Token加入黑名单） |
| GET | `/api/auth/userinfo` | 获取当前用户信息 |

### 设备管理模块 (`/api/devices/**`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/devices/register` | 注册新设备 |
| POST | `/api/devices/{deviceId}/activate` | 激活设备 |
| POST | `/api/devices/{deviceId}/heartbeat` | 设备心跳上报 |
| GET | `/api/devices/{deviceId}` | 查询设备详情 |
| GET | `/api/devices/query` | 分页查询设备列表 |
| PUT | `/api/devices/{deviceId}` | 更新设备信息 |
| DELETE | `/api/devices/{deviceId}` | 删除设备 |
| GET | `/api/devices/{deviceId}/logs` | 查询设备操作日志 |

### IoT 核心模块 (`/api/iot/**`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/iot/devices/{deviceId}/telemetry` | 查询设备遥测历史数据 |
| GET | `/api/iot/devices/{deviceId}/realtime` | WebSocket 实时数据订阅 |
| POST | `/api/iot/devices/{deviceId}/command` | 下发控制命令 |
| GET | `/api/iot/rules` | 查询规则列表 |
| POST | `/api/iot/rules` | 创建规则引擎规则 |
| GET | `/api/iot/health` | 健康检查接口 |

### OTA 升级模块 (`/api/ota/**`, `/api/firmware/**`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/firmware` | 上传固件文件（Multipart） |
| GET | `/api/firmware` | 分页查询固件列表 |
| POST | `/api/firmware/{id}/publish` | 发布固件 |
| POST | `/api/firmware/{id}/archive` | 归档固件 |
| DELETE | `/api/firmware/{id}` | 删除草稿固件 |
| POST | `/api/ota/tasks` | 创建设备升级任务 |
| GET | `/api/ota/tasks/{id}` | 查询任务详情 |
| PUT | `/api/ota/tasks/{id}/status` | 设备上报升级进度 |
| POST | `/api/ota/tasks/{id}/cancel` | 取消升级任务 |

### 数据分析模块 (`/api/analytics/**`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/analytics/dashboard/realtime` | 实时看板数据 |
| GET | `/api/analytics/statistics/daily` | 每日统计数据 |
| GET | `/api/analytics/trend/hourly` | 小时级趋势分析 |
| POST | `/api/analytics/report/export` | 导出报表 |

---

## 🗄️ 数据库设计

### PostgreSQL 业务库

#### 认证库 (`iot_auth`)
- `sys_user` - 用户表（默认账号: admin/admin123）
- `sys_role` - 角色表（ROLE_ADMIN / ROLE_USER）
- `sys_user_role` - 用户角色关联表

#### 设备库 (`iot_device`)
- `device` - 设备主表（device_id唯一、状态、心跳时间、分组等）
- `device_group` - 设备分组表（支持树形层级）
- `device_log` - 设备操作日志表（JSONB存详情）

#### OTA 库 (`iot_ota`)
- `firmware` - 固件表（型号+版本唯一约束、MD5校验、状态流转）
- `ota_task` - OTA任务表（设备升级进度、状态流转、外键关联固件）

### TDengine 时序库
- 超级表设计存储设备遥测数据
- 按设备ID自动建子表
- 支持按月自动分区
- 数据压缩算法优化存储

### Redis 缓存 Key 设计
```
device:status:{deviceId}    # 设备在线状态 (TTL: 300s)
device:info:{deviceId}      # 设备信息缓存 (TTL: 3600s)
device:token:{authToken}    # 设备Token映射
auth:jwt:blacklist:{jti}    # JWT 黑名单
auth:user:session:{userId}  # 用户会话信息
```

### MinIO Bucket 设计
```
iot-logs/          # 设备日志文件
iot-firmware/      # OTA固件升级包
iot-media/         # 监控图片、视频等媒体
iot-backup/        # 数据备份归档
```

---

## 🔐 安全机制

### 认证与授权
1. **JWT 双 Token**：Access Token（24小时）+ Refresh Token（7天）
2. **设备认证**：每设备独立 `auth_token`，Redis 映射校验
3. **RBAC 权限**：用户-角色-权限三级模型（当前已实现基础框架）
4. **网关鉴权**：Gateway 层统一 Token 验证，白名单路径放行
5. **Token 黑名单**：登出后加入 Redis 黑名单，防止失效 Token 复用

### 数据安全
1. **密码加密**：BCrypt 强哈希存储用户密码
2. **传输加密**：HTTPS / MQTTS 生产环境必须启用
3. **固件校验**：MD5 完整性校验，防止篡改
4. **SQL 防护**：MyBatis-Plus 参数化查询，防注入

---

## 📊 数据流示例

### 设备遥测数据上传流程
```
传感器设备
    ↓ MQTT Publish (QoS 1)
    topic: device/{id}/telemetry
EMQX Broker
    ↓ 消息路由
MQTT Subscriber (boonya-io-iot)
    ├─→ 数据校验 & 解析
    ├─→ TDengine 写入（时序存储）
    ├─→ 规则引擎条件匹配
    │      └─→ 触发告警 → AlertHandler
    │                └─→ WebSocket 推送前端
    └─→ 设备状态更新（Redis TTL刷新）
```

### 用户操作流程
```
前端 (Admin/H5)
    ↓ HTTP Request (Header: Authorization: Bearer xxx)
Spring Cloud Gateway (8080)
    ├─→ 跨域处理
    ├─→ JWT Token 验证（白名单跳过）
    ├─→ 限流熔断保护
    └─→ 路由转发 (lb://service-name)
对应微服务 (Auth/Device/OTA...)
    ├─→ 业务逻辑处理
    ├─→ DB/Cache 读写
    └─→ 统一 Result<T> 返回
```

---

## 🧪 测试验证

### 设备注册与心跳测试
```bash
# 1. 用户登录获取 Token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 2. 使用返回的 access_token 注册设备
curl -X POST http://localhost:8080/api/devices/register \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "sensor_temp_001",
    "deviceName": "车间A-温度传感器01号",
    "deviceType": "temperature_sensor",
    "model": "TMP-100",
    "location": "生产车间A区-1号位",
    "description": "监控环境温度 (-40℃ ~ 125℃)"
  }'

# 3. 模拟设备心跳（每5分钟上报一次）
curl -X POST http://localhost:8080/api/devices/sensor_temp_001/heartbeat
```

### MQTT 遥测数据模拟
```bash
# 使用 mosquitto_pub 或 MQTTX 客户端
# Broker: tcp://localhost:1883
# Topic:  device/sensor_temp_001/telemetry
# Payload:
{
  "temperature": 26.8,
  "humidity": 65.2,
  "voltage": 3.31,
  "timestamp": 1716345600000
}
```

---

## 🛠️ 运维监控

### 健康检查
各服务均暴露 Spring Boot Actuator 端点：
```
GET /actuator/health    # 健康状态
GET /actuator/info      # 应用信息
GET /actuator/metrics   # 运行指标（需额外配置暴露）
```

### 日志查看
```bash
# Docker 方式
docker-compose logs -f gateway          # 查看网关日志
docker-compose logs -f iot-app          # 查看IoT核心服务

# 本地运行查看应用控制台输出
```

### 常用运维命令
```bash
# 重启某服务
docker-compose restart auth

# 查看资源占用
docker stats

# 清理停止的容器和悬空镜像
docker system prune -f

# 数据库备份
docker exec -t iot-postgres pg_dumpall -U postgres > backup_$(date +%Y%m%d).sql
```

---

## 📝 开发规范

### 代码规范
- **命名规范**：Java 驼峰命名，SQL/Redis Key 下划线分隔
- **异常处理**：使用自定义 BusinessException/ValidationException，禁止空 catch
- **日志打印**：关键操作 info，异常 error，调试 debug（生产关闭）
- **注释要求**：类/接口必须注释用途，复杂算法必须注释逻辑

### API 响应规范
所有接口统一返回 `Result<T>` 格式：
```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1716345600000
}
```
分页接口统一返回 `PageResult<T>`：
```json
{
  "current": 1,
  "size": 10,
  "total": 156,
  "pages": 16,
  "records": [ ... ]
}
```

### Git 提交规范
```
feat:     新功能
fix:      Bug修复
docs:     文档更新
style:    格式调整（空格分号等）
refactor: 重构（非修复非新增）
perf:     性能优化
test:     测试相关
chore:    构建/工具链变更
```

---

## 📚 相关文档

| 文档 | 路径 | 说明 |
|------|------|------|
| 架构设计文档 | [ARCHITECTURE.md](file:///e:/AI/java/boonya-io/ARCHITECTURE.md) | 详细架构、模块职责、安全设计、部署方案 |
| 公共模块说明 | [boonya-io-common/README.md](file:///e:/AI/java/boonya-io/boonya-io-common/README.md) | 统一响应、异常体系、工具类使用 |
| 设备服务说明 | [boonya-io-device/README.md](file:///e:/AI/java/boonya-io/boonya-io-device/README.md) | 设备服务启动步骤、接口测试示例 |
| OTA服务说明 | [boonya-io-ota/README.md](file:///e:/AI/java/boonya-io/boonya-io-ota/README.md) | 固件管理、OTA任务、数据库设计 |
| IoT服务说明 | [boonya-io-iot/README.md](file:///e:/AI/java/boonya-io/boonya-io-iot/README.md) | MQTT接入、TDengine、设备模拟器 |
| MinIO服务说明 | [boonya-io-minio/README.md](file:///e:/AI/java/boonya-io/boonya-io-minio/README.md) | 对象存储操作说明 |
| 前端项目说明 | [boonya-io-frontend/README.md](file:///e:/AI/java/boonya-io/boonya-io-frontend/README.md) | Admin/H5 功能对比、启动步骤 |
| Admin前端说明 | [boonya-io-frontend/admin/README.md](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/README.md) | 后台管理系统详细说明 |
| H5前端说明 | [boonya-io-frontend/h5/README.md](file:///e:/AI/java/boonya-io/boonya-io-frontend/h5/README.md) | 移动端项目说明 |
| TDengine集成 | [boonya-io-iot/TDENGINE.md](file:///e:/AI/java/boonya-io/boonya-io-iot/TDENGINE.md) | 时序数据库安装与使用 |

---

## 📋 实现状态评估

> **重要说明**：本项目目前处于 **"后端框架完整、前端大量占位"** 的阶段。后端微服务具备真实可用的业务逻辑，但前端存在较多空壳页面和 mock 数据，前后端联调尚未完全贯通。以下为基于源码深度分析的真实实现状态。

### 后端微服务实现状态

| 模块 | 状态 | 接口数 | 真实业务逻辑 | 说明 |
|------|:----:|:------:|:----------:|------|
| boonya-io-gateway | ✅ 可用 | - | JWT鉴权过滤器 + 5条路由 | 白名单放行 + token验签 + 请求头注入 |
| boonya-io-auth | ✅ 完整 | 4 | DB + Redis + JWT + BCrypt | 登录/刷新/登出/注册全链路可用 |
| boonya-io-device | ✅ 完整 | 9 | DB + Redis缓存 + 心跳管理 | 设备CRUD/激活/心跳/分页查询可用 |
| boonya-io-iot | ⚠️ 核心完整 | 7 | MQTT + TDengine + 规则引擎 | 核心链路（订阅→存储→规则→告警）可用；规则管理CRUD有5处TODO |
| boonya-io-minio | ✅ 完整 | 4 | MinIO上传/下载/预签名URL | 文件操作全链路可用 |
| boonya-io-analytics | ✅ 完整 | 3 | TDengine统计聚合查询 | 实时数据/趋势/概览接口可用 |
| boonya-io-ota | ✅ 完整 | 11 | JPA + MinIO + MD5 + 状态机 | 固件管理 + OTA任务全流程可用 |
| boonya-io-cache | ⚠️ 工具库 | 0 | Redis配置类可用，CacheType枚举零引用 | 无启动类，作为工具库存在 |

**后端 TODO 汇总**（共5处，全在 IoT 模块）：
- `RuleController` - 规则列表查询返回空、启用/禁用/删除规则未实现
- `RuleEngine` - FORWARD 转发动作、STORE 特殊存储动作未实现

### 前端实现状态

#### Admin 后台管理

| 页面 | 状态 | 对接后端API | 说明 |
|------|:----:|:----------:|------|
| 登录页 | ✅ 完整 | `/auth/login` | 表单校验 + 登录跳转可用 |
| 主布局/路由/菜单 | ✅ 完整 | - | 侧边栏 + 顶栏 + 路由守卫可用 |
| 请求封装/Pinia Store | ✅ 完整 | - | Axios拦截器 + Token注入可用；缺Token自动刷新 |
| 设备列表 | ✅ 核心可用 | `/devices` | 列表/注册/删除可用；**搜索参数未传后端**；详情/编辑仅toast占位 |
| Dashboard 看板 | ⚠️ UI完整/数据mock | 无 | 4张统计卡片 + 2个ECharts图表 UI完整，**所有数据硬编码** |
| 固件管理 | ❌ 空壳 | 无 | 表格结构在，**4个操作全显示"功能开发中"**，无数据加载 |
| OTA任务 | ❌ 空壳 | 无 | 仅一个 `<el-empty>` 提示 |
| 数据分析 | ❌ 空壳 | 无 | 仅一个 `<el-empty>` 提示 |

#### H5 移动端

| 页面 | 状态 | 对接后端API | 说明 |
|------|:----:|:----------:|------|
| 登录页 | ✅ 完整 | `/auth/login` | Vant表单 + 登录跳转可用 |
| 请求封装 | ✅ 完整 | - | Axios拦截器可用 |
| 设备列表 | ⚠️ 基本可用 | `/devices` | 下拉刷新可用；**分页写死page=1**，上拉加载失效 |
| 设备详情 | ⚠️ UI骨架 | `/devices/{id}` | 仅基本信息展示；**"查看实时数据"按钮无点击事件** |
| 路由守卫 | ❌ 缺失 | - | **未登录可直接访问设备页**，与Admin不一致 |
| 实时数据/控制 | ❌ 未实现 | 无 | 无WebSocket/MQTT对接 |
| 固件/OTA/告警/分析 | ❌ 未实现 | 无 | 路由都不存在 |

### 前后端联调断层清单

| 后端已实现 | 前端状态 | 差距 |
|-----------|---------|------|
| OTA 11个接口（固件+任务全流程） | 前端固件4操作全占位、OTA任务空壳 | **整条OTA业务线前端未实现** |
| Analytics 3个统计接口 | 前端数据分析页空壳 | **数据分析前端完全缺失** |
| IoT 实时数据/规则接口 | 前端无实时数据展示 | **实时监控前端未实现** |
| Device 详情/编辑接口 | 前端仅toast提示 | **设备详情/编辑页未实现** |
| Auth refreshToken接口 | 前端定义了但从未调用 | **Token自动刷新未实现** |
| Device 搜索/分页接口 | 前端搜索参数未传 | **搜索功能形同虚设** |

### 已知技术问题

1. **Dashboard数据假象**：UI看起来完整，但4个统计数字和2个图表全是写死数据（`totalDevices:128, onlineDevices:96`）
2. **H5分页假实现**：`onLoad` 写死 `page:1` 且立即 `finished=true`，上拉加载第二页不会触发
3. **H5无路由守卫**：与Admin行为不一致，存在安全风险
4. **Token刷新机制缺失**：两端 401 直接跳登录页，RefreshToken 形同虚设
5. **类型安全弱**：多处 `any`（`userInfo`、`firmwares`、`deviceInfo`），未利用已定义接口类型
6. **前端API层不统一**：Admin有 `api/` 目录封装，H5 直接在组件里调 `request`
7. **代码重复**：两端 `request.ts` 几乎相同但独立维护

---

## 🎯 后续优化方向

### P0 - 业务功能补全（前端优先）

1. **对接 Dashboard 真实数据**：将4张统计卡片和图表对接 `/api/analytics/overview` 和 `/api/analytics/device/{id}/trend`
2. **实现固件管理前端**：新增 `api/firmware.ts`，实现上传/列表/发布/归档/删除，对接后端11个OTA接口
3. **实现 OTA 任务前端**：任务列表 + 创建任务 + 进度跟踪 + 取消
4. **实现数据分析页**：ECharts图表对接 Analytics 3个接口
5. **实现设备详情/编辑页**：对接 `GET /devices/{id}` 和 `PUT /devices/{id}`
6. **修复设备列表搜索**：将 `deviceName`/`status` 参数传入 `loadDevices`
7. **H5实现实时数据**：WebSocket/MQTT.js 对接 IoT 服务，展示设备实时遥测

### P1 - 功能完善

8. **Token自动刷新**：401时用RefreshToken换取新Token，无感刷新
9. **H5路由守卫**：与Admin一致，未登录跳转登录页
10. **H5分页修复**：正确实现 `page` 递增和 `finished` 判断
11. **规则引擎CRUD补全**：实现后端5处TODO（规则列表/启用/禁用/删除/FORWARD/STORE）
12. **前端API层统一**：H5 也封装 `api/` 目录，提取公共类型

### P2 - 架构演进

13. **服务注册发现**：集成 Nacos 作为注册配置中心，替代静态路由
14. **分布式事务**：跨服务操作引入 Seata 保证最终一致性
15. **消息队列**：引入 Kafka/RocketMQ 削峰填谷，处理海量设备并发
16. **链路追踪**：集成 SkyWalking / Zipkin 实现分布式链路追踪
17. **指标监控**：Prometheus + Grafana 可视化服务运行指标
18. **日志中心**：ELK Stack 统一收集检索微服务日志
19. **K8s 部署**：生产环境迁移 Kubernetes，实现弹性伸缩
20. **规则引擎增强**：完善规则引擎 DSL，支持更复杂的联动逻辑
21. **AI 智能分析**：利用 Spring AI 接入大模型，实现异常预测和智能诊断
22. **多租户支持**：SaaS 化改造，按租户隔离数据和权限

---

## 📄 License

Copyright © 2026 Boonya Lab. All rights reserved.

---

**文档版本**: v1.1  
**最后更新**: 2026-08-04  
**维护团队**: Boonya Lab Team
