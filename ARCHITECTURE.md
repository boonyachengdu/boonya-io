# Boonya IoT 平台 - 架构设计文档

## 📐 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                      客户端层 (Clients)                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ Web 前端  │  │ 移动APP  │  │ 第三方API│  │ 设备端   │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/WebSocket/MQTT
┌────────────────────────▼────────────────────────────────────┐
│                   API 网关层 (Gateway)                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Spring Cloud Gateway                                 │  │
│  │  - 路由转发  - 负载均衡  - 限流熔断  - 统一鉴权       │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                    微服务层 (Microservices)                   │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Auth Service │  │Device Service│  │ IoT Core     │      │
│  │  (8083)      │  │  (38080)     │  │  (18080)     │      │
│  │              │  │              │  │              │      │
│  │ - JWT 认证   │  │ - 设备注册   │  │ - MQTT 接入  │      │
│  │ - 用户管理   │  │ - 状态监控   │  │ - 规则引擎   │      │
│  │ - 权限控制   │  │ - 分组管理   │  │ - 告警服务   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │Analytics Svc │  │ MinIO Service│  │ OTA Service  │      │
│  │  (8084)      │  │  (8082)      │  │  (8085)      │      │
│  │              │  │              │  │              │      │
│  │ - 数据看板   │  │ - 文件存储   │  │ - 固件管理   │      │
│  │ - 统计分析   │  │ - 日志归档   │  │ - 升级调度   │      │
│  │ - 报表导出   │  │ - CDN 加速   │  │ - 版本控制   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                  数据存储层 (Data Storage)                    │
│                                                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │PostgreSQL│  │ TDengine │  │  Redis   │  │  MinIO   │   │
│  │          │  │          │  │          │  │          │   │
│  │业务数据  │  │时序数据  │  │缓存会话  │  │对象存储  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## 🏗️ 模块详细说明

### 1. boonya-io-common (公共模块)
**职责**: 提供所有模块共享的基础设施

#### 核心功能
- **统一响应封装**: `Result<T>`, `PageResult<T>`
- **异常体系**: 业务异常、资源未找到、验证异常
- **全局异常处理**: 统一的错误响应格式
- **工具类**: JSON 序列化、Token 生成
- **常量定义**: 设备状态、MQTT Topic、Redis Key

#### 使用示例
```java
// 统一响应
return Result.success(device);
return Result.error("Invalid device ID");

// 异常抛出
throw new ResourceNotFoundException("Device", deviceId);
throw new ValidationException("deviceId", "格式不正确");
```

---

### 2. boonya-io-device (设备管理模块)
**职责**: 设备的生命周期管理

#### 核心功能
- **设备注册**: 创建设备并生成认证 Token
- **设备激活**: 激活新注册的设备
- **心跳管理**: 维护设备在线状态
- **状态监控**: 实时查询设备状态（Redis 缓存）
- **分组管理**: 设备分组和标签管理
- **日志记录**: 设备操作日志

#### 数据模型
```sql
-- 设备表
device (id, device_id, device_name, device_type, model, 
        firmware_version, status, last_heartbeat, group_id, 
        location, description, auth_token)

-- 设备分组表
device_group (id, group_name, group_code, parent_id, description)

-- 设备日志表
device_log (id, device_id, log_type, message, detail, create_time)
```

#### 缓存策略
```
Redis Key 设计:
- device:status:{deviceId} -> online/offline (TTL: 300s)
- device:info:{deviceId} -> Device JSON (TTL: 3600s)
- device:token:{authToken} -> deviceId (TTL: 永久)
```

---

### 3. boonya-io-auth (认证授权模块)
**职责**: 用户认证、授权和会话管理

#### 核心功能
- **JWT 认证**: 基于 Token 的无状态认证
- **用户管理**: 用户注册、登录、信息管理
- **RBAC 权限**: 角色-based 访问控制
- **Token 刷新**: 双 Token 机制（Access + Refresh）
- **会话管理**: Redis 存储黑名单 Token

#### 认证流程
```
1. 用户登录 → POST /api/auth/login
2. 验证用户名密码 → 生成 Access Token (24h) + Refresh Token (7d)
3. 后续请求携带 Access Token → Header: Authorization: Bearer {token}
4. Token 过期 → 使用 Refresh Token 刷新 → POST /api/auth/refresh
5. 登出 → Token 加入黑名单 (Redis)
```

#### 安全配置
```yaml
jwt:
  secret: <强密钥>
  expiration: 86400000      # Access Token 24小时
  refresh-expiration: 604800000  # Refresh Token 7天
```

---

### 4. boonya-io-iot (IoT 核心模块)
**职责**: 设备接入、消息处理、规则引擎、告警

#### 核心功能
- **MQTT 接入**: 支持 EMQX 和嵌入式 Moquette
- **消息订阅**: 通配符匹配 (`device/+/telemetry`)
- **时序存储**: TDengine 存储设备遥测数据
- **规则引擎**: 基于条件的自动化规则（待完善）
- **告警服务**: WebSocket 实时推送告警
- **设备模拟**: 100个虚拟设备生成测试数据

#### MQTT Topic 设计
```
上行主题（设备 → 云端）:
- device/{deviceId}/telemetry    # 遥测数据
- device/{deviceId}/event        # 事件上报
- device/{deviceId}/log          # 日志上报

下行主题（云端 → 设备）:
- device/{deviceId}/command      # 控制命令
- device/{deviceId}/config       # 配置下发
- device/{deviceId}/ota          # OTA 升级指令
```

#### 数据流
```
设备 → MQTT Broker → MqttSubscriber → 解析数据
                                    ↓
                          TDengine (时序存储)
                                    ↓
                          规则引擎 (条件判断)
                                    ↓
                          AlertHandler (告警触发)
                                    ↓
                          WebSocket (前端推送)
```

---

### 5. boonya-io-minio (对象存储模块)
**职责**: 非结构化数据存储

#### 核心功能
- **文件上传**: 支持大文件分片上传
- **文件下载**: 预签名 URL 临时访问
- **设备日志**: 存储设备日志文件
- **固件存储**: OTA 固件包存储
- **图片视频**: 监控截图、视频片段

#### Bucket 设计
```
- iot-logs/          # 设备日志文件
- iot-firmware/      # 固件升级包
- iot-media/         # 媒体文件（图片、视频）
- iot-backup/        # 数据备份
```

---

### 6. boonya-io-gateway (API 网关) [待实现]
**职责**: 统一入口、路由、鉴权、限流

#### 核心功能
- **路由转发**: 根据路径转发到对应微服务
- **负载均衡**: 服务多实例负载分发
- **统一鉴权**: JWT Token 验证
- **限流熔断**: 防止服务过载
- **请求日志**: 审计和监控

#### 路由配置示例
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://boonya-io-auth
          predicates:
            - Path=/api/auth/**
        
        - id: device-service
          uri: lb://boonya-io-device
          predicates:
            - Path=/api/devices/**
        
        - id: iot-service
          uri: lb://boonya-io-iot
          predicates:
            - Path=/api/iot/**
```

---

### 7. boonya-io-analytics (数据分析) [待实现]
**职责**: 数据可视化、统计分析、报表

#### 核心功能
- **实时看板**: ECharts 实时数据展示
- **历史查询**: 多维度数据查询
- **统计分析**: 聚合计算（平均值、最大值等）
- **趋势预测**: 基于历史数据的预测
- **报表导出**: Excel/PDF 报表生成

#### 典型接口
```
GET /api/analytics/dashboard/realtime?deviceId=xxx
GET /api/analytics/statistics/daily?deviceId=xxx&date=2026-05-22
GET /api/analytics/trend/hourly?deviceId=xxx
POST /api/analytics/report/export
```

---

### 8. boonya-io-ota (固件升级) [待实现]
**职责**: 远程固件升级管理

#### 核心功能
- **固件管理**: 版本管理、上传、审核
- **升级策略**: 全量升级、灰度升级、分批升级
- **进度跟踪**: 实时监控升级进度
- **失败回滚**: 自动回滚到上一版本
- **断点续传**: 支持大文件断点续传

#### 升级流程
```
1. 上传固件包 → MinIO 存储
2. 创建升级任务 → 选择目标设备和策略
3. 下发升级指令 → MQTT: device/{id}/ota
4. 设备下载固件 → HTTP 从 MinIO 下载
5. 设备刷写固件 → 报告进度
6. 升级完成 → 验证新版本
7. 失败回滚 → 恢复旧版本
```

---

## 🔐 安全设计

### 认证与授权
1. **设备认证**: 每个设备拥有唯一的 `auth_token`
2. **用户认证**: JWT Token (Access + Refresh)
3. **API 鉴权**: Gateway 层统一验证
4. **权限控制**: RBAC 模型（角色-权限）

### 数据安全
1. **传输加密**: HTTPS/TLS
2. **密码加密**: BCrypt 哈希
3. **敏感数据**: 数据库字段加密
4. **Token 管理**: Redis 黑名单机制

### 网络安全
1. **限流**: Gateway 层 IP 限流、用户限流
2. **CORS**: 跨域资源共享控制
3. **防火墙**: 只开放必要端口
4. **DDoS 防护**: 云服务商防护 + 应用层防护

---

## 📊 数据流设计

### 设备数据采集流程
```
1. 设备采集传感器数据
   ↓
2. 通过 MQTT 发布到 topic: device/{id}/telemetry
   ↓
3. MQTT Broker 接收消息
   ↓
4. MqttSubscriber 订阅并解析
   ↓
5. 数据存储到 TDengine (时序数据库)
   ↓
6. 规则引擎判断是否触发告警
   ↓
7. 如有告警 → WebSocket 推送到前端
   ↓
8. Analytics 模块聚合统计
   ↓
9. 前端看板实时展示
```

### 用户操作流程
```
1. 用户登录 → Auth Service 验证
   ↓
2. 获取 JWT Token
   ↓
3. 访问 API → Gateway 验证 Token
   ↓
4. 路由到对应微服务
   ↓
5. 业务逻辑处理
   ↓
6. 返回结果
```

---

## 🚀 部署架构

### Docker Compose 部署
```yaml
services:
  # 基础设施
  postgres:      # PostgreSQL (业务数据)
  tdengine:      # TDengine (时序数据)
  redis:         # Redis (缓存)
  emqx:          # EMQX (MQTT Broker)
  minio:         # MinIO (对象存储)
  
  # 微服务
  gateway:       # API 网关 (8080)
  auth:          # 认证服务 (8083)
  device:        # 设备管理 (38080)
  iot:           # IoT 核心 (18080)
  minio-service: # MinIO 服务 (8082)
  analytics:     # 数据分析 (8084)
  ota:           # OTA 升级 (8085)
```

### Kubernetes 部署（生产环境）
```
Namespace: boonya-iot

Deployments:
- gateway-deployment ( replicas: 2 )
- auth-deployment ( replicas: 2 )
- device-deployment ( replicas: 2 )
- iot-deployment ( replicas: 3 )
- analytics-deployment ( replicas: 2 )

StatefulSets:
- postgres-statefulset
- tdengine-statefulset
- redis-statefulset
- minio-statefulset

Services:
- gateway-service (LoadBalancer)
- 各微服务 ClusterIP

Ingress:
- iot.boonya.com → gateway-service
```

---

## 📈 性能优化

### 缓存策略
1. **Redis 缓存**:
   - 设备状态（TTL: 5分钟）
   - 用户会话（TTL: 24小时）
   - 热点数据（TTL: 1小时）

2. **本地缓存**:
   - Caffeine 缓存频繁访问的配置
   - Guava Cache 临时数据

### 数据库优化
1. **TDengine**:
   - 超级表 + 子表设计
   - 数据分区（按月）
   - 压缩算法

2. **PostgreSQL**:
   - 索引优化
   - 连接池（HikariCP）
   - 读写分离（可选）

### 消息队列（可选）
- Kafka 缓冲高并发消息
- 异步处理设备数据
- 削峰填谷

---

## 📝 开发规范

### 代码规范
1. **命名**: 驼峰命名法，语义清晰
2. **注释**: 关键逻辑必须注释
3. **异常**: 使用自定义异常，禁止吞掉异常
4. **日志**: 关键操作记录日志

### API 规范
1. **RESTful**: 遵循 REST 风格
2. **版本控制**: `/api/v1/...`
3. **统一响应**: 使用 `Result<T>` 封装
4. **分页**: 使用 `PageResult<T>`

### Git 规范
1. **分支**: main, develop, feature/*, hotfix/*
2. **提交**: Conventional Commits
   - `feat: add device registration`
   - `fix: resolve token validation bug`
   - `docs: update API documentation`

---

## 🔍 监控与运维

### 可观测性
1. **日志**: ELK Stack (Elasticsearch + Logstash + Kibana)
2. **指标**: Prometheus + Grafana
3. **链路**: SkyWalking / Zipkin

### 健康检查
```
GET /actuator/health
GET /actuator/metrics
GET /actuator/info
```

### 告警
- 服务宕机告警
- CPU/内存超限告警
- 磁盘空间不足告警
- 异常率突增告警

---

**文档版本**: v1.0  
**最后更新**: 2026-05-22  
**维护者**: Boonya Lab Team
