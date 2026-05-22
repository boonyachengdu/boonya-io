# Boonya IoT 平台 - 模块实现清单

## ✅ 后端模块（8/8 全部完成）

### 1. boonya-io-common (公共模块)
**状态**: ✅ 已完成
**位置**: `D:\code\boonya-io\boonya-io-common`

#### 核心文件
```
src/main/java/com/boonya/lab/io/common/
├── config/
│   └── GlobalExceptionHandler.java       # 全局异常处理器
├── constant/
│   └── CommonConstants.java              # 通用常量
├── exception/
│   ├── BusinessException.java            # 业务异常
│   ├── ResourceNotFoundException.java    # 资源未找到异常
│   └── ValidationException.java          # 验证异常
├── response/
│   ├── Result.java                       # 统一响应结果
│   └── PageResult.java                   # 分页响应结果
└── util/
    ├── JsonUtils.java                    # JSON 工具
    └── TokenUtils.java                   # Token 生成工具
```

---

### 2. boonya-io-auth (认证授权模块)
**状态**: ✅ 已完成
**端口**: 8083
**位置**: `D:\code\boonya-io\boonya-io-auth`

#### 核心文件
```
src/main/java/com/boonya/lab/io/auth/
├── AuthApplication.java                  # 主应用类
├── controller/
│   └── AuthController.java               # 认证控制器
├── dto/
│   ├── LoginRequest.java                 # 登录请求
│   └── LoginResponse.java                # 登录响应
├── entity/
│   └── User.java                         # 用户实体
├── mapper/
│   └── UserMapper.java                   # 用户 Mapper
├── service/
│   └── AuthService.java                  # 认证服务
└── util/
    └── JwtUtils.java                     # JWT 工具类
```

#### API 接口
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/refresh` - 刷新 Token
- `POST /api/auth/logout` - 用户登出
- `POST /api/auth/register` - 用户注册

---

### 3. boonya-io-device (设备管理模块)
**状态**: ✅ 已完成
**端口**: 38080
**位置**: `D:\code\boonya-io\boonya-io-device`

#### 核心文件
```
src/main/java/com/boonya/lab/io/device/
├── DeviceApplication.java                # 主应用类
├── config/
│   └── MybatisPlusConfig.java            # MyBatis-Plus 配置
├── controller/
│   └── DeviceController.java             # 设备管理控制器
├── dto/
│   ├── DeviceQueryRequest.java           # 设备查询请求
│   ├── DeviceRegisterRequest.java        # 设备注册请求
│   └── DeviceResponse.java               # 设备响应
├── entity/
│   ├── Device.java                       # 设备实体
│   ├── DeviceGroup.java                  # 设备分组实体
│   └── DeviceLog.java                    # 设备日志实体
├── mapper/
│   ├── DeviceMapper.java                 # 设备 Mapper
│   ├── DeviceGroupMapper.java            # 分组 Mapper
│   └── DeviceLogMapper.java              # 日志 Mapper
└── service/
    └── DeviceService.java                # 设备服务
```

#### API 接口
- `POST /api/devices/register` - 注册设备
- `POST /api/devices/{deviceId}/activate` - 激活设备
- `POST /api/devices/{deviceId}/heartbeat` - 设备心跳
- `GET /api/devices/{id}` - 获取设备信息
- `GET /api/devices/query` - 查询设备列表（分页）
- `GET /api/devices/online` - 获取在线设备
- `GET /api/devices/{deviceId}/status` - 获取设备状态
- `PUT /api/devices/{id}` - 更新设备状态
- `DELETE /api/devices/{id}` - 删除设备

---

### 4. boonya-io-iot (IoT 核心模块)
**状态**: ✅ 已完成
**端口**: 18080
**位置**: `D:\code\boonya-io\boonya-io-iot`

#### 核心文件
```
src/main/java/com/boonya/lab/io/iot/
├── IotApplication.java                   # 主应用类
├── config/
│   ├── EmbeddedMqttBroker.java           # 嵌入式 MQTT Broker
│   ├── EmqxClientConfig.java             # EMQX 客户端配置
│   ├── MqttBrokerProperties.java         # MQTT Broker 属性
│   └── WebSocketConfig.java              # WebSocket 配置
├── constant/
│   └── NettyConfig.java                  # Netty 配置常量
├── controller/
│   ├── DeviceController.java             # 设备控制器
│   ├── HealthController.java             # 健康检查
│   ├── HomeController.java               # 首页
│   └── RuleController.java               # 规则引擎控制器
├── device/
│   └── DeviceSimulator.java              # 设备模拟器
├── event/
│   ├── OverTempEvent.java                # 温度超限事件
│   └── handler/
│       └── AlertHandler.java             # 告警处理器
├── model/
│   ├── DeviceData.java                   # 设备数据模型
│   └── DeviceLog.java                    # 设备日志模型
├── mqtt/
│   ├── MqttClientWrapper.java            # MQTT 客户端接口
│   └── impl/
│       ├── EmbeddedMqttClientWrapper.java # 嵌入式 MQTT 实现
│       └── EmqxMqttClientWrapper.java    # EMQX MQTT 实现
└── ruleengine/
    ├── Rule.java                         # 规则定义
    └── RuleEngine.java                   # 规则引擎
```

#### 核心功能
- ✅ MQTT Broker（EMQX + 嵌入式 Moquette 双方案）
- ✅ 时序数据存储（TDengine）
- ✅ WebSocket 实时推送
- ✅ 规则引擎（条件判断与事件触发）
- ✅ 告警服务
- ✅ 设备模拟器

---

### 5. boonya-io-analytics (数据分析模块)
**状态**: ✅ 已完成
**端口**: 8084
**位置**: `D:\code\boonya-io\boonya-io-analytics`

#### 核心文件
```
src/main/java/com/boonya/lab/io/analytics/
├── AnalyticsApplication.java             # 主应用类
├── controller/
│   └── DashboardController.java          # 看板数据控制器
├── dto/
│   └── DeviceRealtimeData.java           # 设备实时数据 DTO
└── service/
    └── DashboardService.java             # 看板服务
```

#### API 接口
- `GET /api/analytics/device/{deviceId}/realtime` - 设备实时数据
- `GET /api/analytics/device/{deviceId}/trend` - 设备趋势数据
- `GET /api/analytics/overview` - 系统概览

---

### 6. boonya-io-minio (对象存储模块)
**状态**: ✅ 已完成
**端口**: 8082
**位置**: `D:\code\boonya-io\boonya-io-minio`

#### 核心文件
```
src/main/java/com/boonya/lab/io/minio/
├── MinioApplication.java                 # 主应用类
├── config/
│   ├── MinioConfig.java                  # MinIO 配置
│   └── MinioProperties.java              # MinIO 属性
├── controller/
│   ├── FileController.java               # 文件控制器
│   └── HomeController.java               # 首页
└── service/
    └── MinioService.java                 # MinIO 服务
```

---

### 7. boonya-io-gateway (API 网关)
**状态**: ✅ 已完成
**端口**: 8080
**位置**: `D:\code\boonya-io\boonya-io-gateway`

#### 核心文件
```
src/main/java/com/boonya/lab/io/gateway/
├── GatewayApplication.java               # 主应用类
└── filter/
    └── AuthenticationFilter.java         # JWT 认证过滤器
```

#### 路由配置
| 路径 | 目标服务 | 端口   |
|------|---------|------|
| `/api/auth/**` | auth-service | 8083 |
| `/api/devices/**` | device-service | 8086 |
| `/api/iot/**` | iot-service | 8081 |
| `/api/analytics/**` | analytics-service | 8084 |
| `/api/files/**` | minio-service | 8082 |
| `/api/firmware/**`, `/api/ota/**` | ota-service | 8085 |

---

### 8. boonya-io-ota (固件升级模块)
**状态**: ✅ 已完成
**端口**: 8085
**位置**: `D:\code\boonya-io\boonya-io-ota`

#### 核心文件
```
src/main/java/com/boonya/lab/io/ota/
├── OtaApplication.java                   # 主应用类
├── config/
│   └── MinioConfig.java                  # MinIO 配置
├── controller/
│   ├── FirmwareController.java           # 固件控制器
│   └── OtaTaskController.java            # OTA 任务控制器
├── dto/
│   └── FirmwareUploadRequest.java        # 上传请求 DTO
├── entity/
│   ├── Firmware.java                     # 固件实体
│   └── OtaTask.java                      # OTA 任务实体
├── repository/
│   ├── FirmwareRepository.java           # 固件仓库
│   └── OtaTaskRepository.java            # 任务仓库
└── service/
    ├── FirmwareService.java              # 固件服务
    └── OtaTaskService.java              # 任务服务
```

#### API 接口
**固件管理**：
- `POST /api/firmware` - 上传固件
- `GET /api/firmware` - 获取固件列表
- `GET /api/firmware/{id}` - 获取固件详情
- `POST /api/firmware/{id}/publish` - 发布固件
- `POST /api/firmware/{id}/archive` - 归档固件
- `DELETE /api/firmware/{id}` - 删除固件

**OTA 任务**：
- `POST /api/ota/tasks` - 创建任务
- `GET /api/ota/tasks/{id}` - 获取任务详情
- `GET /api/ota/tasks/device/{deviceId}` - 获取设备任务列表
- `PUT /api/ota/tasks/{id}/status` - 更新任务状态
- `POST /api/ota/tasks/{id}/cancel` - 取消任务

---

## ✅ 前端模块

### boonya-io-frontend/admin (管理后台)
**状态**: ✅ 已完成
**技术栈**: Vue 3 + TypeScript
**位置**: `D:\code\boonya-io\boonya-io-frontend\admin`

### boonya-io-frontend/h5 (移动端)
**状态**: ✅ 已完成
**技术栈**: Vue 3 + TypeScript
**位置**: `D:\code\boonya-io\boonya-io-frontend\h5`

---

## 🔧 技术栈总览

| 模块 | 技术栈 | 端口   | 状态 |
|------|--------|------|------|
| common | Spring Boot, Jackson | -    | ✅ |
| gateway | Spring Cloud Gateway, JWT | 8080 | ✅ |
| iot | Spring Boot, MQTT, TDengine, WebSocket | 8081 | ✅ |
| minio | Spring Boot, MinIO SDK | 8082 | ✅ |
| auth | Spring Boot, JWT, MyBatis-Plus, PostgreSQL, Redis | 8083 | ✅ |
| analytics | Spring Boot, TDengine | 8084 | ✅ |
| ota | Spring Boot, JPA, PostgreSQL, MinIO | 8085 | ✅ |
| device | Spring Boot, MyBatis-Plus, PostgreSQL, Redis | 8086 | ✅ |
| frontend/admin | Vue 3 + TypeScript | -    | ✅ |
| frontend/h5 | Vue 3 + TypeScript | -    | ✅ |

---

## 📚 参考文档

- [项目整体进展](./PROJECT_STATUS.md)
- [架构设计文档](./ARCHITECTURE.md)
- [完成总结](./COMPLETION_SUMMARY.md)

---

**最后更新**: 2026-05-22
**维护者**: Boonya Lab Team
