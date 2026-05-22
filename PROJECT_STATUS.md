# Boonya IoT 平台 - 项目进展

## 📊 当前状态

### ✅ 已完成的模块

#### 1. boonya-io-common (公共模块)
**状态**: ✅ 已完成  
**功能**:
- 统一响应封装 (`Result<T>`, `PageResult<T>`)
- 异常体系 (`BusinessException`, `ResourceNotFoundException`, `ValidationException`)
- 全局异常处理器 (`GlobalExceptionHandler`)
- 工具类 (`JsonUtils`, `TokenUtils`)
- 常量定义 (`CommonConstants`)

**使用方式**:
```xml
<dependency>
    <groupId>com.boonya.lab.io</groupId>
    <artifactId>boonya-io-common</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

#### 2. boonya-io-device (设备管理模块)
**状态**: ✅ 已完成  
**端口**: 38080  
**功能**:
- 设备注册/激活/注销
- 设备心跳管理
- 设备状态监控（在线/离线）
- 设备分组管理
- 设备日志记录
- Redis 缓存集成

**API 接口**:
- `POST /api/devices/register` - 注册设备
- `POST /api/devices/{deviceId}/activate` - 激活设备
- `POST /api/devices/{deviceId}/heartbeat` - 设备心跳
- `GET /api/devices/{id}` - 获取设备信息
- `GET /api/devices/query` - 查询设备列表
- `GET /api/devices/online` - 获取在线设备
- `DELETE /api/devices/{id}` - 删除设备

**技术栈**:
- Spring Boot 3.3.5
- MyBatis-Plus 3.5.9
- PostgreSQL (设备数据存储)
- Redis (状态缓存)
- Swagger/Knife4j (API 文档)

#### 3. boonya-io-iot (IoT 核心模块)
**状态**: ⚠️ 已有基础功能，待重构  
**当前功能**:
- MQTT Broker (嵌入式 Moquette + EMQX 双方案)
- 时序数据存储 (TDengine)
- WebSocket 实时推送
- 设备模拟器 (100个虚拟设备)
- 简单告警机制

**待改进**:
- 拆分为 mqtt-handler、rule-engine、alert-service 子模块
- 集成规则引擎
- 完善告警通知渠道

#### 4. boonya-io-minio (对象存储模块)
**状态**: ✅ 已完成  
**功能**:
- 文件上传/下载
- 设备日志存储
- MinIO 集成

---

### 🚧 待实现的模块

#### 5. boonya-io-auth (认证授权模块)
**优先级**: ⭐⭐⭐⭐⭐  
**计划功能**:
- JWT Token 认证
- Spring Security 集成
- 用户管理
- RBAC 权限控制
- OAuth2 支持（可选）

#### 6. boonya-io-gateway (API 网关模块)
**优先级**: ⭐⭐⭐⭐⭐  
**计划功能**:
- Spring Cloud Gateway
- 路由转发
- 负载均衡
- 限流熔断
- 统一鉴权

#### 7. boonya-io-analytics (数据分析模块)
**优先级**: ⭐⭐⭐⭐  
**计划功能**:
- 实时数据看板
- 历史数据查询
- 数据聚合统计
- ECharts 可视化
- 报表导出

#### 8. boonya-io-ota (固件升级模块)
**优先级**: ⭐⭐⭐  
**计划功能**:
- 固件版本管理
- OTA 升级包上传 (MinIO)
- 灰度发布
- 升级进度跟踪
- 失败回滚

---

## 🏗️ 项目架构

```
boonya-io/
├── boonya-io-common/          # ✅ 公共模块
├── boonya-io-device/          # ✅ 设备管理 (Port: 38080)
├── boonya-io-iot/             # ⚠️ IoT 核心 (Port: 18080) - 待重构
├── boonya-io-minio/           # ✅ 对象存储 (Port: 8082)
├── boonya-io-auth/            # 🚧 认证授权 - 待实现
├── boonya-io-gateway/         # 🚧 API 网关 - 待实现
├── boonya-io-analytics/       # 🚧 数据分析 - 待实现
└── boonya-io-ota/             # 🚧 OTA 升级 - 待实现
```

---

## 🔧 技术栈总览

| 类别 | 技术 | 版本 |
|------|------|------|
| **基础框架** | Spring Boot | 3.3.5 |
| **微服务** | Spring Cloud | 2023.0.3 |
| **数据库** | PostgreSQL | 15+ |
| **时序数据库** | TDengine | Latest |
| **缓存** | Redis | Alpine |
| **ORM** | MyBatis-Plus | 3.5.9 |
| **MQTT** | EMQX / Moquette | Latest |
| **对象存储** | MinIO | Latest |
| **API 文档** | Swagger + Knife4j | 4.5.0 |
| **Java** | JDK | 17 |

---

## 📝 下一步计划

### Phase 1: 核心完善 (1-2周)
1. ✅ ~~完成 common 模块~~
2. ✅ ~~完成 device 模块~~
3. 🔄 重构 iot 模块（拆分 mqtt-handler、rule-engine、alert-service）
4. 🔲 实现 auth 模块（JWT + Spring Security）

### Phase 2: 网关与监控 (2-3周)
5. 🔲 实现 gateway 模块（Spring Cloud Gateway）
6. 🔲 实现 analytics 模块（数据可视化）
7. 🔲 集成 Prometheus + Grafana（监控）

### Phase 3: 高级功能 (3-4周)
8. 🔲 实现 ota 模块（固件升级）
9. 🔲 完善规则引擎（Drools/LiteFlow）
10. 🔲 多租户支持

### Phase 4: 部署优化 (1-2周)
11. 🔲 统一 docker-compose.yml 编排
12. 🔲 Kubernetes 部署配置
13. 🔲 CI/CD 流水线

---

## 🚀 快速启动

### 前置要求
- JDK 17+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 15+
- Redis
- TDengine

### 启动基础设施
```bash
# 启动 PostgreSQL
docker run -d --name postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -v postgres-data:/var/lib/postgresql/data \
  postgres:15

# 启动 Redis
docker run -d --name redis -p 6379:6379 redis:alpine

# 启动 TDengine
docker run -d --name tdengine \
  -p 6030:6030 -p 6041:6041 -p 6060:6060 \
  tdengine/tdengine:latest

# 启动 MinIO
docker run -d --name minio \
  -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"

# 启动 EMQX (可选)
docker run -d --name emqx \
  -p 1883:1883 -p 18083:18083 \
  emqx/emqx:latest
```

### 初始化数据库
```bash
# 创建 iot_device 数据库
psql -U postgres -h localhost -c "CREATE DATABASE iot_device;"

# 执行 schema.sql
psql -U postgres -h localhost -d iot_device -f boonya-io-device/src/main/resources/schema.sql
```

### 编译项目
```bash
cd D:\code\boonya-io
mvn clean install -DskipTests
```

### 启动服务
```bash
# 启动设备管理模块
cd boonya-io-device
mvn spring-boot:run

# 访问 Swagger 文档
http://localhost:38080/swagger-ui.html
```

---

## 📖 文档

- [Common 模块使用说明](./boonya-io-common/README.md)
- [Device 模块 API 文档](http://localhost:38080/swagger-ui.html)
- [IoT 模块文档](./boonya-io-iot/README.md)
- [MinIO 模块文档](./boonya-io-minio/README.md)

---

## 🤝 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 📄 License

MIT License

---

**最后更新**: 2026-05-22  
**维护者**: Boonya Lab Team
