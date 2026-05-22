# Boonya IoT 平台 - 模块实现清单

## ✅ 已完成的模块

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

### 2. boonya-io-device (设备管理模块)
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

### 3. boonya-io-auth (认证授权模块)
**状态**: 🔄 部分完成  
**端口**: 8083  
**位置**: `D:\code\boonya-io\boonya-io-auth`

#### 已创建的文件
```
src/main/java/com/boonya/lab/io/auth/
├── AuthApplication.java                  # ✅ 主应用类
├── util/
│   └── JwtUtils.java                     # ✅ JWT 工具类
└── entity/
    └── User.java                         # ✅ 用户实体
```

#### 需要补充的文件
```
src/main/java/com/boonya/lab/io/auth/
├── config/
│   ├── SecurityConfig.java               # ⏳ Spring Security 配置
│   └── MybatisPlusConfig.java            # ⏳ MyBatis-Plus 配置
├── controller/
│   └── AuthController.java               # ⏳ 认证控制器
├── dto/
│   ├── LoginRequest.java                 # ⏳ 登录请求
│   ├── LoginResponse.java                # ⏳ 登录响应
│   ├── RegisterRequest.java              # ⏳ 注册请求
│   └── UserInfoResponse.java             # ⏳ 用户信息响应
├── entity/
│   ├── Role.java                         # ⏳ 角色实体
│   └── UserRole.java                     # ⏳ 用户角色关联
├── mapper/
│   ├── UserMapper.java                   # ⏳ 用户 Mapper
│   ├── RoleMapper.java                   # ⏳ 角色 Mapper
│   └── UserRoleMapper.java               # ⏳ 用户角色 Mapper
├── service/
│   ├── AuthService.java                  # ⏳ 认证服务
│   └── UserService.java                  # ⏳ 用户服务
└── security/
    ├── JwtAuthenticationFilter.java      # ⏳ JWT 认证过滤器
    └── UserDetailsServiceImpl.java       # ⏳ 用户详情服务
```

---

## 🚧 待实现的模块

### 4. boonya-io-gateway (API 网关)
**优先级**: ⭐⭐⭐⭐⭐  
**计划端口**: 8080

#### 需要创建的核心文件
```
src/main/java/com/boonya/lab/io/gateway/
├── GatewayApplication.java
├── config/
│   ├── GatewayConfig.java                # 路由配置
│   └── CorsConfig.java                   # CORS 配置
└── filter/
    ├── AuthenticationFilter.java         # 认证过滤器
    └── LoggingFilter.java                # 日志过滤器
```

---

### 5. boonya-io-analytics (数据分析模块)
**优先级**: ⭐⭐⭐⭐  
**计划端口**: 8084

#### 需要创建的核心文件
```
src/main/java/com/boonya/lab/io/analytics/
├── AnalyticsApplication.java
├── controller/
│   ├── DashboardController.java          # 看板数据
│   └── ReportController.java             # 报表数据
├── service/
│   ├── DashboardService.java             # 看板服务
│   └── StatisticsService.java            # 统计服务
└── dto/
    ├── DashboardData.java                # 看板数据 DTO
    └── StatisticsResult.java             # 统计结果 DTO
```

---

### 6. boonya-io-ota (固件升级模块)
**优先级**: ⭐⭐⭐  
**计划端口**: 8085

#### 需要创建的核心文件
```
src/main/java/com/boonya/lab/io/ota/
├── OtaApplication.java
├── controller/
│   ├── FirmwareController.java           # 固件管理
│   └── UpgradeController.java            # 升级管理
├── service/
│   ├── FirmwareService.java              # 固件服务
│   └── UpgradeService.java               # 升级服务
└── entity/
    ├── Firmware.java                     # 固件实体
    └── UpgradeRecord.java                # 升级记录实体
```

---

## 📝 下一步行动

### 立即可做
1. **完善 auth 模块** - 补充剩余的 Service、Controller、Security 配置
2. **创建数据库脚本** - 为 auth 模块创建 PostgreSQL schema
3. **测试 device 模块** - 启动并测试设备管理 API

### 短期计划（1-2周）
4. **实现 gateway 模块** - Spring Cloud Gateway + 路由 + 鉴权
5. **重构 iot 模块** - 拆分为 mqtt-handler、rule-engine、alert-service
6. **集成所有模块** - 统一 docker-compose.yml 编排

### 中期计划（2-4周）
7. **实现 analytics 模块** - 数据可视化 + 报表
8. **实现 ota 模块** - 固件升级管理
9. **完善规则引擎** - Drools/LiteFlow 集成

---

## 🔧 技术栈总览

| 模块 | 技术栈 | 端口 | 状态 |
|------|--------|------|------|
| common | Spring Boot, Jackson | - | ✅ |
| device | Spring Boot, MyBatis-Plus, PostgreSQL, Redis | 38080 | ✅ |
| auth | Spring Boot, Spring Security, JWT, PostgreSQL | 8083 | 🔄 |
| iot | Spring Boot, MQTT, TDengine, WebSocket | 18080 | ⚠️ |
| minio | Spring Boot, MinIO SDK | 8082 | ✅ |
| gateway | Spring Cloud Gateway | 8080 (计划) | 🚧 |
| analytics | Spring Boot, ECharts | 8084 (计划) | 🚧 |
| ota | Spring Boot, MinIO | 8085 (计划) | 🚧 |

---

## 📚 参考文档

- [Common 模块使用说明](./boonya-io-common/README.md)
- [项目整体进展](./PROJECT_STATUS.md)
- [IoT 模块文档](./boonya-io-iot/README.md)
- [MinIO 模块文档](./boonya-io-minio/README.md)

---

**最后更新**: 2026-05-22  
**维护者**: Boonya Lab Team
