# Boonya IoT 专业能力迭代计划 v4（增强版）

> 范围：在 v3 计划基础上，补充探索发现的遗漏项，形成完整的专业 IoT 平台闭环
> 原则：功能严格衔接、不漏功能、前后端联动、每个 Phase 可独立验证
> 基础：v3 计划的 5 Phase 框架不变，在每个 Phase 中补充遗漏项并强化衔接

---

## 一、当前状态分析（基于代码探索）

### 1.1 已实现能力

| 模块 | 已实现 | 端口 |
|------|--------|------|
| auth | 用户/角色/权限 RBAC、JWT 登录/刷新/登出、5 张 sys_* 表 | 8083 |
| device | Device CRUD（10 API）、Alert 管理（6 API）、DeviceGroup/DeviceLog 仅实体无业务层 | 8086 |
| iot | MQTT 订阅（硬编码 temp）、TDengine 时序存储（2 张超表）、RuleEngine（单设备阈值）、WebSocket、AI 分析（统计）、设备模拟器（100 温度+5 能碳） | 8081 |
| gateway | JWT 认证过滤器（仅用户级）、8 条路由、Redis 限流、CORS | 8080 |
| ota | 固件上传/发布/归档（6 API）、OTA 任务记录（6 API），无下载/无 MQTT 下发/无版本回写 | 8085 |
| analytics | TDengine 聚合查询、能碳分析（硬编码设备档案）、看板 API | 8084 |
| minio | 文件上传/下载 | 8082 |
| 前端 admin | 14 个页面、MQTT 单例订阅、Pinia 状态管理、v-permission 指令 | 3000 |

### 1.2 完整差距清单（18 项）

#### v3 已识别的 15 项差距

| # | 差距 | 影响 | v3 Phase |
|---|------|------|----------|
| 1 | 物模型完全缺失 | 数据解析硬编码 `json.get("temp")`，无法泛化 | Phase 1 |
| 2 | 设备影子缺失 | 离线设备无法控制，期望/上报状态无法同步 | Phase 2 |
| 3 | 设备认证形同虚设 | EMQX 允许匿名，gateway 不验证 device token | Phase 1（仅 LWT） |
| 4 | 设备分组无 API | DeviceGroup 仅有实体+Mapper，无 Controller/Service | Phase 3 |
| 5 | 无离线检测定时任务 | 设备断连后状态永远停留在 ONLINE | Phase 3 |
| 6 | OTA 固件下载接口缺失 | 设备端无法获取固件二进制 | Phase 3 |
| 7 | OTA 版本不回写 | 升级成功后 Device.firmwareVersion 不更新 | Phase 3 |
| 8 | 无批量设备导入 | 仅支持单个注册 | Phase 3 |
| 9 | 场景联动缺失 | 规则仅单设备阈值告警，无跨设备联动 | Phase 4 |
| 10 | 定时规则缺失 | 无 cron 触发能力 | Phase 4 |
| 11 | 设备指令下发缺失 | 无法通过 MQTT 向设备下发控制命令 | Phase 2 |
| 12 | 告警通知渠道单一 | 仅 WebSocket/MQTT，无邮件/Webhook | Phase 4 |
| 13 | 无 GIS 地图 | 无设备地理分布可视化 | Phase 5 |
| 14 | 无设备拓扑图 | 无网关-子设备关系可视化 | Phase 5 |
| 15 | MQTT LWT/Retained 缺失 | 设备异常断连无通知，上线无法获取最后状态 | Phase 1 |

#### v4 新增的 3 项差距（探索发现）

| # | 差距 | 影响 | 归属 Phase |
|---|------|------|------------|
| 16 | DeviceLog 业务层缺失 | device 模块有 DeviceLog 实体+Mapper+表，但无 Controller/Service，设备日志无法查询 | Phase 3 |
| 17 | DeviceSimulator 与物模型未联动 | 模拟器硬编码 100 个温度传感器 + 5 个能碳设备，物模型建成后模拟器不按物模型生成数据 | Phase 1 |
| 18 | 能碳分析硬编码设备档案 | EnergyAnalyticsService 中 DEVICE_PROFILES 静态 List 写死 5 台设备，无法泛化 | Phase 1 |

#### 附加技术债务（贯穿各 Phase 修复）

| # | 债务 | 修复时机 |
|---|------|----------|
| A | iot 模块 PostgreSQL 密码 `boonya` 与 docker-compose `postgres` 不一致 | Phase 1 |
| B | EMQX 允许匿名访问，无 ACL | Phase 1 |
| C | 前端 DeviceDetail 未接入 MQTT 实时数据 | Phase 5 |
| D | 前端无路由级权限拦截（仅元素级 v-permission） | Phase 5 |
| E | OTA 用 JPA，其他模块用 MyBatis-Plus（技术栈不统一，暂不强制统一） | 不改 |
| F | Kafka DeviceDataProducer 已定义但无人调用（预留，本期不启用） | 不改 |

---

## 二、实施计划（5 个 Phase，严格按序执行）

### Phase 1：物模型体系 + 设备认证（IoT 平台根基）

> 物模型是后续数据解析、规则引擎、指令下发的数据基础，必须最先完成。
> 设备认证与物模型同属"接入层"，合并到本 Phase 一起补齐。

#### 1.1 后端：物模型数据模型 + API（device 模块）

**新建表**（追加到 `docker/postgres/init-databases.sh` 的 iot_device 库初始化段）：
```sql
-- 产品（设备模板）
CREATE TABLE IF NOT EXISTS iot_product (
    id BIGSERIAL PRIMARY KEY,
    product_key VARCHAR(64) UNIQUE NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    node_type VARCHAR(16) DEFAULT 'DIRECT',
    protocol_type VARCHAR(16) DEFAULT 'MQTT',
    data_format VARCHAR(16) DEFAULT 'JSON',
    description TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 物模型属性
CREATE TABLE IF NOT EXISTS iot_thing_model (
    id BIGSERIAL PRIMARY KEY,
    product_key VARCHAR(64) NOT NULL,
    identifier VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    data_type VARCHAR(16) NOT NULL,
    unit VARCHAR(32),
    min_value DOUBLE PRECISION,
    max_value DOUBLE PRECISION,
    access_mode VARCHAR(16) DEFAULT 'RW',
    description TEXT,
    sort INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_key, identifier)
);

-- 物模型服务
CREATE TABLE IF NOT EXISTS iot_thing_service (
    id BIGSERIAL PRIMARY KEY,
    product_key VARCHAR(64) NOT NULL,
    identifier VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    call_type VARCHAR(16) DEFAULT 'ASYNC',
    input_params TEXT,
    output_params TEXT,
    description TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_key, identifier)
);
```

**新建文件**（device 模块）：
- `entity/Product.java` — 产品实体（@TableName("iot_product")）
- `entity/ThingModel.java` — 物模型属性实体（@TableName("iot_thing_model")）
- `entity/ThingService.java` — 物模型服务实体（@TableName("iot_thing_service")）
- `mapper/ProductMapper.java` — 继承 BaseMapper<Product>
- `mapper/ThingModelMapper.java` — 继承 BaseMapper<ThingModel>
- `mapper/ThingServiceMapper.java` — 继承 BaseMapper<ThingService>
- `service/ProductService.java` — 产品 CRUD + 物模型属性/服务管理
- `controller/ProductController.java` — 产品 + 物模型 API
  ```
  GET    /api/products                         产品分页列表
  POST   /api/products                         创建产品
  GET    /api/products/{productKey}            产品详情
  PUT    /api/products/{productKey}            更新产品
  DELETE /api/products/{productKey}            删除产品
  GET    /api/products/{productKey}/properties 物模型属性列表
  POST   /api/products/{productKey}/properties 添加属性
  PUT    /api/thing-model/properties/{id}      更新属性
  DELETE /api/thing-model/properties/{id}      删除属性
  GET    /api/products/{productKey}/services   物模型服务列表
  POST   /api/products/{productKey}/services   添加服务
  PUT    /api/thing-model/services/{id}        更新服务
  DELETE /api/thing-model/services/{id}        删除服务
  ```

**修改文件**：
- `device/entity/Device.java` — 添加 `productKey`（String）字段
- `docker/postgres/init-databases.sh` — device 表添加 `product_key VARCHAR(64)` 列 + 新增 3 张物模型表
- `device/dto/DeviceRegisterRequest.java` — 添加 `productKey` 字段（可选）

#### 1.2 后端：基于物模型的数据解析（iot 模块）

**新建文件**：
- `iot/service/ThingModelCacheService.java` — 物模型缓存服务
  - `getProperties(productKey)` — 从 device 模块 Feign 获取物模型属性，缓存到 Redis（TTL 5 分钟）
  - `parsePayload(productKey, payload)` — 根据物模型解析 JSON payload 为 `Map<String, Object>` 属性 Map
  - 缓存 Key：`iot:thing-model:{productKey}`

**修改文件**：`iot/service/MqttSubscriber.java`
- `handleDeviceData` 方法重构：
  - 通过 deviceId 查询设备的 productKey（Feign 调用 device 服务 `/api/devices/by-id/{deviceId}`）
  - 调用 `ThingModelCacheService.parsePayload(productKey, payload)` 解析多属性
  - 遍历属性 Map，每个属性调用 `TimeSeriesService.saveProperty(deviceId, identifier, value, ts)`
  - 同步更新设备影子 reported_state（Phase 2 实现，此处预留接口调用）
  - 触发 RuleEngine.evaluate 时传入完整属性 Map（替代仅 temp）

**修改文件**：`iot/service/TimeSeriesService.java`
- 新增 `saveProperty(deviceId, identifier, value, ts)` 方法
- TDengine 超表 schema 扩展：新增通用属性超表
  ```sql
  CREATE STABLE IF NOT EXISTS iot.device_properties (
      ts TIMESTAMP,
      prop_value DOUBLE
  ) TAGS (device_id NCHAR(32), identifier NCHAR(64))
  ```
- 子表命名：`iot.p_{safeDeviceId}_{safeIdentifier}`
- 保留原 `iot.devices`（温度）和 `iot.energy_metrics` 超表兼容历史数据
- 新增 `queryProperties(deviceId, identifiers, startTs, endTs)` 查询方法

**修改文件**：`iot/device/DeviceSimulator.java`（差距 #17）
- 启动时通过 Feign 查询所有产品及其物模型
- 若物模型存在，按物模型属性定义生成随机数据（温度/湿度/开关等）
- 若物模型不存在，降级为当前的 100 温度传感器模式
- 能碳设备模拟保留（与物模型无关，走 energy 通道）

#### 1.3 后端：设备认证 + EMQX 安全（iot + gateway + device 模块）

**修改文件**：`iot/config/EmqxClientConfig.java`
- `MqttConnectOptions` 添加 `setUserName(admin)/setPassword(public)`（使用配置凭据）
- 添加 LWT 遗嘱消息：`MqttConnectOptions.setWill("iot/service/offline", payload, 1, false)`
  - payload: `{"clientId":"iot-service","timestamp":<ts>}`
- 服务端断连时通知（可选）

**新建文件**：`iot/service/DeviceAuthService.java` — 设备认证服务
- `validateDevice(deviceId, authToken)` — 验证设备 token 有效性（Feign 调用 device 服务）
- `recordDeviceOnline(deviceId)` — 记录设备上线（Redis SET + TTL）
- `recordDeviceOffline(deviceId)` — 记录设备离线

**修改文件**：`iot/config/EmqxClientConfig.java`（差距 #15 LWT）
- 为每个连接的设备维护 LWT（需 EMQX 侧配置，见下）

**修改文件**：`docker-compose.yml`（差距 #B EMQX 安全）
- EMQX 环境变量修改：
  ```yaml
  EMQX_ALLOW_ANONYMOUS: "false"   # 禁用匿名
  ```
- 新增 EMQX 认证配置挂载（通过 EMQX REST API 初始化或挂载 `emqx_authn.conf`）
- 本期采用简化方案：EMQX 启用用户名密码认证（admin/public），所有设备共用同一凭据
- 后续可扩展为 EMQX HTTP 认证插件调用 device 服务验证 device token

**修改文件**：`iot/src/main/resources/application-docker.yml`（差距 #A 密码不一致）
- `pg.datasource.password: boonya` → `pg.datasource.password: postgres`

#### 1.4 后端：能碳分析去硬编码（analytics 模块）（差距 #18）

**修改文件**：`analytics/service/EnergyAnalyticsService.java`
- 移除 `DEVICE_PROFILES` 静态 List
- 改为通过 Feign 调用 device 服务查询能碳设备列表（deviceType IN ('electric_meter','water_meter','solar_inverter','energy_storage')）
- 设备档案从 DB 动态加载，缓存到 Redis（TTL 10 分钟）
- 碳排放因子和电价保留为配置项（application.yml）

**修改文件**：`analytics/application-docker.yml`
- 添加 device 服务 Feign 配置

#### 1.5 后端：网关路由

**修改文件**：`gateway/src/main/resources/application-docker.yml`
- 添加 `/api/products/**` 路由到 device 服务
- 添加 `/api/thing-model/**` 路由到 device 服务

#### 1.6 前端：物模型管理页面

**新建文件**：
- `admin/src/api/product.ts` — 产品 + 物模型 API
  ```typescript
  getProductList, createProduct, getProduct, updateProduct, deleteProduct,
  getThingModelProperties, createProperty, updateProperty, deleteProperty,
  getThingModelServices, createService, updateService, deleteService
  ```
- `admin/src/views/device/ProductList.vue` — 产品列表页（CRUD + 查看物模型入口）
- `admin/src/views/device/ProductDetail.vue` — 产品详情（基本信息 + 物模型属性 Tab + 物模型服务 Tab）
- `admin/src/views/device/ThingModelPropertyEditor.vue` — 物模型属性编辑弹窗组件
- `admin/src/views/device/ThingModelServiceEditor.vue` — 物模型服务编辑弹窗组件

**修改文件**：
- `admin/src/router/index.ts` — 添加路由
  ```typescript
  { path: 'products', name: 'ProductList', meta: { title: '产品管理', icon: 'Box' } },
  { path: 'products/:productKey', name: 'ProductDetail', meta: { title: '产品详情', hidden: true } },
  ```
- `admin/src/views/devices/DeviceList.vue` — 注册设备弹窗添加产品选择下拉框
- `admin/src/views/devices/DeviceDetail.vue` — 添加「物模型」Tab，展示设备所属产品的属性/服务

---

### Phase 2：设备影子 + 指令下发（设备控制闭环）

> 物模型定义了设备的"读写"能力，设备影子实现"写"的落地，指令下发实现"写"的传输。

#### 2.1 后端：设备影子（iot 模块）

**新建表**（追加到 `docker/postgres/init-databases.sh`）：
```sql
CREATE TABLE IF NOT EXISTS device_shadow (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(64) NOT NULL UNIQUE,
    desired_state TEXT,
    reported_state TEXT,
    version BIGINT DEFAULT 0,
    last_desired_time TIMESTAMP,
    last_reported_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**新建文件**：
- `iot/model/DeviceShadow.java` — 影子 POJO（desired/reported/version/metadata）
- `iot/service/DeviceShadowService.java`
  - `getShadow(deviceId)` — 获取设备影子
  - `updateDesired(deviceId, properties)` — 更新期望状态（version+1，发布 MQTT）
  - `updateReported(deviceId, properties)` — 更新上报状态（Phase 1 预留接口的实现）
  - `clearDesired(deviceId)` — 清除期望状态（设备同步后）
  - 影子变更后通过 MQTT 发布到 `device/{deviceId}/shadow`

**修改文件**：
- `iot/service/MqttSubscriber.java` — 设备上报数据时调用 `DeviceShadowService.updateReported`（Phase 1 预留的接口）

#### 2.2 后端：设备指令下发（iot 模块）

**新建表**：
```sql
CREATE TABLE IF NOT EXISTS device_command (
    id BIGSERIAL PRIMARY KEY,
    command_id VARCHAR(64) NOT NULL,
    device_id VARCHAR(64) NOT NULL,
    service_identifier VARCHAR(64),
    params TEXT,
    status VARCHAR(16) DEFAULT 'PENDING',
    result TEXT,
    sent_time TIMESTAMP,
    ack_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**新建文件**：
- `iot/service/DeviceCommandService.java`
  - `sendCommand(deviceId, commandId, serviceIdentifier, params)` — 通过 MQTT 下发服务调用指令
  - 发布到 `device/{deviceId}/command` 主题，payload 含 commandId + serviceId + params
  - 记录指令到 DB
  - `getCommandHistory(deviceId)` — 查询指令历史
  - `updateCommandResult(commandId, result)` — 更新指令执行结果
- `iot/controller/DeviceCommandController.java`
  ```
  POST /api/iot/devices/{deviceId}/command    下发指令
  GET  /api/iot/devices/{deviceId}/commands   指令历史
  GET  /api/iot/devices/{deviceId}/shadow     获取设备影子
  PUT  /api/iot/devices/{deviceId}/shadow     更新期望状态
  ```

**修改文件**：
- `iot/service/MqttSubscriber.java` — 订阅 `device/+/command/resp` 主题，接收设备指令响应，调用 `DeviceCommandService.updateCommandResult`

#### 2.3 前端：设备影子 + 指令下发页面

**新建文件**：
- `admin/src/api/device-control.ts` — 设备影子 + 指令 API
- `admin/src/views/devices/DeviceShadow.vue` — 设备影子面板组件
  - 左列：期望状态（可编辑，属性值输入框）
  - 右列：上报状态（只读，实时更新）
  - 底部：版本号 + 最后更新时间 + 同步按钮
- `admin/src/views/devices/DeviceControl.vue` — 设备控制面板组件
  - 列出物模型中的可写属性 + 可调用服务
  - 属性设置：输入框 + 设置按钮（调用影子 updateDesired）
  - 服务调用：参数表单 + 调用按钮（调用指令下发）

**修改文件**：
- `admin/src/views/devices/DeviceDetail.vue` — 添加「设备影子」Tab 和「设备控制」Tab

---

### Phase 3：设备管理增强（运营效率提升）

#### 3.1 后端：设备分组 CRUD（device 模块）（差距 #4）

**新建文件**：
- `device/service/DeviceGroupService.java` — 分组树管理
- `device/controller/DeviceGroupController.java`
  ```
  GET    /api/device-groups              分组树
  POST   /api/device-groups              创建分组
  PUT    /api/device-groups/{id}         更新分组
  DELETE /api/device-groups/{id}         删除分组
  PUT    /api/devices/{deviceId}/group   设备移动到分组
  GET    /api/device-groups/{id}/devices 分组下设备列表
  ```

#### 3.2 后端：设备日志业务层（device 模块）（差距 #16）

**新建文件**：
- `device/service/DeviceLogService.java` — 设备日志查询
- `device/controller/DeviceLogController.java`
  ```
  GET /api/devices/{deviceId}/logs  设备日志分页查询（支持 logType/时间范围筛选）
  ```
- 内部接口：`recordLog(deviceId, logType, message, detail)` — 供其他服务调用记录日志

#### 3.3 后端：离线检测定时任务（device 模块）（差距 #5）

**新建文件**：
- `device/job/DeviceStatusCheckJob.java`
  - `@Scheduled(fixedRate = 60000)` — 每分钟扫描
  - 检查 Redis 中设备心跳 TTL，超时设备标记 OFFLINE
  - 设备状态变更时发布 MQTT 通知 `device/{deviceId}/status`
  - 记录设备日志（调用 DeviceLogService）

**修改文件**：
- `device/DeviceApplication.java` — 添加 `@EnableScheduling`

#### 3.4 后端：OTA 闭环补齐（ota 模块）（差距 #6, #7）

**修改文件**：
- `ota/controller/FirmwareController.java` — 添加下载接口
  ```
  GET /api/firmware/{id}/download  生成 MinIO 预签名下载 URL（有效期 1 小时）
  ```
- `ota/service/FirmwareService.java` — 添加 `generateDownloadUrl(firmwareId)` 方法
- `ota/service/OtaTaskService.java`
  - `createOtaTask` 成功后，通过 MQTT 发布升级命令到 `device/{deviceId}/ota/upgrade`
  - `updateTaskStatus` 成功时，调用 device 服务更新 `Device.firmwareVersion`

**新建文件**：
- `ota/service/DeviceIntegrationService.java` — Feign 调用 device 服务同步固件版本
- `ota/config/MqttClientConfig.java` — OTA 模块独立的 MQTT 客户端配置（连接 EMQX）

**修改文件**：
- `ota/pom.xml` — 添加 `spring-integration-mqtt` 或 Paho 依赖
- `ota/application-docker.yml` — 添加 MQTT 配置

#### 3.5 后端：批量设备导入（device 模块）（差距 #8）

**新建文件**：
- `device/controller/DeviceBatchController.java`
  ```
  POST /api/devices/batch-import    Excel/CSV 批量导入
  GET  /api/devices/import-template 下载导入模板
  ```
- `device/service/DeviceBatchImportService.java` — Apache POI 解析 Excel，批量注册设备

**修改文件**：
- `device/pom.xml` — 添加 `org.apache.poi:poi-ooxml` 依赖

#### 3.6 前端：设备管理增强页面

**新建文件**：
- `admin/src/api/device-group.ts` — 分组 API
- `admin/src/views/devices/DeviceGroupTree.vue` — 分组树组件（可嵌入 DeviceList 左侧）
- `admin/src/views/devices/DeviceImport.vue` — 批量导入弹窗（el-upload + 模板下载）

**修改文件**：
- `admin/src/views/devices/DeviceList.vue`
  - 左侧添加分组树筛选
  - 工具栏添加「批量导入」按钮
- `admin/src/views/firmware/FirmwareList.vue` — 添加「下载」按钮
- `admin/src/views/ota/OtaTaskList.vue` — 任务创建时显示固件版本信息

**前端依赖**：
- `admin/package.json` — 添加 `xlsx` 依赖（Excel 解析）

---

### Phase 4：规则引擎增强 + 多渠道告警

> 依赖 Phase 1 的物模型（属性定义）和 Phase 2 的指令下发（COMMAND 动作）。

#### 4.1 后端：场景联动引擎（iot 模块）（差距 #9）

**新建表**：
```sql
CREATE TABLE IF NOT EXISTS scene_linkage (
    id BIGSERIAL PRIMARY KEY,
    scene_id VARCHAR(64) NOT NULL UNIQUE,
    scene_name VARCHAR(128) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    trigger_type VARCHAR(16) NOT NULL,
    trigger_config TEXT,
    condition_config TEXT,
    action_config TEXT,
    execute_count BIGINT DEFAULT 0,
    last_execute_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**新建文件**：
- `iot/model/Scene.java` — 场景实体
- `iot/model/SceneAction.java` — 动作定义（ALERT/COMMAND/WEBHOOK/SHADOW_UPDATE）
- `iot/service/SceneLinkageService.java`
  - `evaluate(deviceId, data)` — 设备数据上报时评估场景触发条件（在 MqttSubscriber 中调用）
  - `executeActions(scene, deviceId, data)` — 执行动作列表
    - ALERT: 调用 AlertManageService.createAlert
    - COMMAND: 调用 DeviceCommandService.sendCommand
    - WEBHOOK: HTTP POST 到外部 URL
    - SHADOW_UPDATE: 调用 DeviceShadowService.updateDesired
  - `executeTimer(scene)` — 定时触发
- `iot/controller/SceneController.java`
  ```
  GET    /api/scenes            场景列表
  POST   /api/scenes            创建场景
  PUT    /api/scenes/{id}       更新场景
  DELETE /api/scenes/{id}       删除场景
  PUT    /api/scenes/{id}/toggle 启用/禁用
  POST   /api/scenes/{id}/execute 手动触发
  ```

**修改文件**：
- `iot/service/MqttSubscriber.java` — `handleDeviceData` 中调用 `SceneLinkageService.evaluate`

#### 4.2 后端：定时规则调度（iot 模块）（差距 #10）

**新建文件**：
- `iot/job/SceneTimerScheduler.java`
  - 使用 Spring `TaskScheduler`
  - 启动时从 DB 加载 enabled 且 trigger_type=TIMER 的场景
  - 按 cron 表达式调度执行
  - 场景增删改时动态更新调度

**修改文件**：
- `iot/IotApplication.java` — 添加 `@EnableScheduling`

#### 4.3 后端：多渠道告警通知（device 模块）（差距 #12）

**新建表**：
```sql
CREATE TABLE IF NOT EXISTS alert_notification_channel (
    id BIGSERIAL PRIMARY KEY,
    channel_name VARCHAR(128) NOT NULL,
    channel_type VARCHAR(16) NOT NULL,
    config TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS alert_notification_rule (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT,
    channel_id BIGINT,
    severity_filter VARCHAR(16),
    enabled BOOLEAN DEFAULT TRUE
);
```

**新建文件**：
- `device/service/NotificationService.java`
  - `sendNotification(alert, channels)` — 根据渠道发送通知
  - `sendEmail(to, subject, body)` — 邮件通知（Spring Mail）
  - `sendWebhook(url, payload)` — HTTP POST Webhook
  - `sendDingTalk(webhook, message)` — 钉钉机器人
- `device/controller/NotificationController.java`
  ```
  GET    /api/notification/channels       渠道列表
  POST   /api/notification/channels       创建渠道
  PUT    /api/notification/channels/{id}  更新渠道
  DELETE /api/notification/channels/{id}  删除渠道
  POST   /api/notification/channels/{id}/test 测试发送
  ```

**修改文件**：
- `device/service/AlertManageService.java` — `createAlert` 后调用 `NotificationService`（通过 Feign 或 HTTP 调用，因 AlertManageService 在 device 模块，NotificationService 也在 device 模块，直接注入）
- `device/pom.xml` — 添加 `spring-boot-starter-mail` 依赖
- `device/application-docker.yml` — 添加邮件配置（SMTP）

#### 4.4 前端：场景联动 + 通知配置页面

**新建文件**：
- `admin/src/api/scene.ts` — 场景联动 API
- `admin/src/api/notification.ts` — 通知渠道 API
- `admin/src/views/automation/SceneList.vue` — 场景列表页
- `admin/src/views/automation/SceneEditor.vue` — 场景编辑器
  - 触发条件区：选择设备 + 属性 + 运算符 + 阈值，或定时 cron
  - 过滤条件区：时间范围/设备分组
  - 动作编排区：可添加多个动作（告警/指令下发/Webhook/影子更新）
- `admin/src/views/system/NotificationConfig.vue` — 通知渠道配置页

**修改文件**：
- `admin/src/router/index.ts` — 添加路由
  ```typescript
  { path: 'automation/scenes', name: 'SceneList', meta: { title: '场景联动', icon: 'Connection' } },
  { path: 'automation/scenes/:id', name: 'SceneEditor', meta: { title: '场景编辑', hidden: true } },
  { path: 'system/notifications', name: 'NotificationConfig', meta: { title: '通知配置', icon: 'Message' } },
  ```

---

### Phase 5：可视化大屏 + 前端增强

#### 5.1 后端：设备位置 + 拓扑数据 API

**修改文件**：
- `device/entity/Device.java` — 添加 `longitude`(Double) / `latitude`(Double) 字段
- `docker/postgres/init-databases.sh` — device 表添加 longitude/latitude 列
- `device/controller/DeviceController.java`
  ```
  GET /api/devices/map       返回所有设备的位置+状态+最新数据
  GET /api/devices/topology  返回网关-子设备拓扑结构
  ```
- `device/service/DeviceService.java` — 添加 `getDeviceMap()` 和 `getDeviceTopology()` 方法

**修改文件**：
- `iot/service/MqttSubscriber.java` — 网关设备上报时，解析子设备数据（如有）

#### 5.2 前端：GIS 地图大屏（差距 #13）

**新建文件**：
- `admin/src/views/visualization/DeviceMap.vue` — 设备地图大屏
  - 使用 Leaflet + OpenStreetMap（开源无 API Key 依赖）
  - 设备标记点：颜色区分在线/离线/告警
  - 点击标记弹出设备信息卡片
  - 左侧统计面板：总数/在线/离线/告警
  - 支持地图缩放和拖拽

**前端依赖**：
- `admin/package.json` — 添加 `leaflet` 依赖

#### 5.3 前端：设备拓扑图（差距 #14）

**新建文件**：
- `admin/src/views/visualization/DeviceTopology.vue` — 设备拓扑可视化
  - 使用 ECharts Graph 图（已有 ECharts 依赖）
  - 网关设备为根节点，子设备为叶子
  - 节点颜色表示状态（绿=在线/灰=离线/红=告警）
  - 点击节点跳转设备详情

#### 5.4 前端：实时数据大屏

**新建文件**：
- `admin/src/views/visualization/RealtimeDashboard.vue` — 全屏实时大屏
  - 顶部：系统标题 + 时间
  - 左侧：设备统计 + 告警统计
  - 中间：GIS 地图（复用 DeviceMap 组件）
  - 右侧：实时告警滚动列表 + 设备温度趋势
  - 底部：能碳数据概览
  - 支持 MQTT 实时数据刷新

#### 5.5 前端增强（差距 #C, #D）

**修改文件**：
- `admin/src/views/devices/DeviceDetail.vue`（差距 #C）
  - 历史数据 Tab 接入 MQTT 实时订阅 `device/{deviceId}/telemetry`
  - 实时追加趋势图数据点
- `admin/src/router/index.ts`（差距 #D）
  - 为需要权限的路由添加 `meta.permissions` 字段
  - 路由守卫中校验 `meta.permissions` 与 `userStore.permissions`
- `admin/src/router/index.ts` — 添加可视化路由
  ```typescript
  { path: 'visualization/map', name: 'DeviceMap', meta: { title: '设备地图', icon: 'Location' } },
  { path: 'visualization/topology', name: 'DeviceTopology', meta: { title: '设备拓扑', icon: 'Share' } },
  { path: 'visualization/dashboard', name: 'RealtimeDashboard', meta: { title: '实时大屏', icon: 'DataBoard' } },
  ```
- `admin/src/layouts/MainLayout.vue` — 菜单添加「可视化大屏」分组（使用 el-sub-menu 嵌套）

---

## 三、功能衔接矩阵（确保不漏功能）

```
Phase 1: 物模型 + 设备认证
├── 物模型定义 (product_key, properties, services)
│   ├── → MqttSubscriber: 根据 productKey 动态解析 payload（替代硬编码 temp）
│   ├── → TimeSeriesService: 通用属性超表 device_properties
│   ├── → DeviceSimulator: 按物模型生成模拟数据
│   ├── → 前端设备详情: 展示物模型 Tab
│   ├── → Phase 2 指令下发: 调用物模型定义的服务
│   ├── → Phase 4 场景联动: 条件基于物模型属性
│   └── → Phase 1 能碳分析: 去硬编码，从 DB 加载设备档案
├── 设备认证 (EMQX 禁用匿名 + LWT)
│   ├── → 设备上线/离线追踪
│   └── → Phase 3 离线检测: 配合定时任务
└── 配置修复 (iot pg 密码)

Phase 2: 设备影子 + 指令下发
├── 设备影子 (desired/reported)
│   ├── → MqttSubscriber: 上报数据时更新 reported（Phase 1 预留接口）
│   ├── → 前端设备影子 Tab: desired/reported 双列对比
│   └── → Phase 4 场景联动: SHADOW_UPDATE 动作
├── 指令下发 (MQTT publish command)
│   ├── → 前端设备控制 Tab: 调用物模型服务
│   ├── → Phase 3 OTA: 升级命令通过 MQTT 下发
│   └── → Phase 4 场景联动: COMMAND 动作
└── 指令响应 (订阅 command/resp)
    └── → DeviceCommandService.updateCommandResult

Phase 3: 设备管理增强
├── 设备分组 (CRUD + 树)
│   ├── → 前端 DeviceList 左侧分组树筛选
│   └── → Phase 4 场景联动: 过滤条件支持分组
├── 设备日志 (Controller/Service) ← 补齐 v3 遗漏
│   └── → 前端 DeviceDetail 操作日志 Tab（已有，改用真实 API）
├── 离线检测定时任务
│   ├── → 设备状态变更 → MQTT 通知 → 前端实时更新
│   └── → 记录设备日志
├── OTA 闭环
│   ├── → 固件下载接口
│   ├── → MQTT 下发升级命令 → 设备收到 → 上报进度
│   └── → 版本回写 device.firmwareVersion
└── 批量导入 (Excel)
    └── → 前端 DeviceList 批量导入按钮

Phase 4: 规则引擎增强 + 多渠道告警
├── 场景联动 (DEVICE/TIMER/MANUAL 触发)
│   ├── 触发: 设备数据（物模型属性）/ 定时 / 手动
│   ├── 条件: 属性阈值 / 时间范围 / 设备分组
│   └── 动作:
│       ├── ALERT → AlertManageService.createAlert
│       ├── COMMAND → DeviceCommandService.sendCommand (Phase 2)
│       ├── WEBHOOK → HTTP POST
│       └── SHADOW_UPDATE → DeviceShadowService.updateDesired (Phase 2)
├── 定时规则调度
│   └── → 场景增删改时动态更新调度
└── 多渠道告警通知
    ├── → AlertManageService.createAlert 后触发
    ├── → 邮件/Webhook/钉钉
    └── → 前端通知渠道配置页

Phase 5: 可视化大屏 + 前端增强
├── GIS 地图 (Leaflet + OSM)
│   ├── → 设备位置 + 状态 + 告警
│   └── → 复用到实时大屏
├── 设备拓扑图 (ECharts Graph)
│   └── → 网关-子设备关系
├── 实时大屏
│   ├── → MQTT 实时数据 + 告警滚动
│   └── → 复用 DeviceMap 组件
├── DeviceDetail 接入实时数据 ← 补齐 v3 遗漏
└── 路由级权限拦截 ← 补齐 v3 遗漏
```

---

## 四、假设与决策

| # | 决策点 | 选择 | 理由 |
|---|--------|------|------|
| 1 | 物模型放哪个模块 | device 模块 | 物模型与产品/设备强关联 |
| 2 | 设备影子存储方式 | PostgreSQL 表 | 需要持久化 + 版本管理，Redis 仅做缓存 |
| 3 | 指令下发通道 | MQTT publish | 与设备数据上报通道一致，设备端只需一个 MQTT 连接 |
| 4 | 场景联动引擎 | 独立 SceneLinkageService | 与 RuleEngine 并行，RuleEngine 保持简单阈值告警 |
| 5 | 定时规则实现 | Spring TaskScheduler | 支持动态 cron 注册/注销 |
| 6 | GIS 地图方案 | Leaflet + OpenStreetMap | 开源无 API Key 依赖，避免高德地图申请 Key 的麻烦 |
| 7 | 设备拓扑图 | ECharts Graph | 无需额外依赖，项目已有 ECharts |
| 8 | 批量导入格式 | Excel(.xlsx) | 企业用户习惯，Apache POI 解析 |
| 9 | OTA 版本回写方式 | Feign 调用 device 服务 | 避免跨模块 DB 直连，保持服务边界 |
| 10 | 告警通知渠道优先级 | Email + Webhook + 钉钉 | 覆盖企业最常用渠道 |
| 11 | EMQX 认证方案 | 禁用匿名 + 用户名密码认证（admin/public） | 本期简化方案，所有设备共用凭据；后续可扩展 HTTP 认证插件 |
| 12 | TDengine 通用属性存储 | 新增 device_properties 超表 | 保留原 devices 超表兼容历史，新属性走通用超表 |
| 13 | 能碳分析去硬编码方式 | Feign 调用 device 服务 | 保持服务边界，Redis 缓存设备档案 |
| 14 | 设备模拟器联动物模型 | 启动时查询物模型，按定义生成数据 | 物模型不存在时降级为当前模式 |
| 15 | 前端 Excel 解析 | xlsx 库 | 纯前端解析，无需后端参与模板下载 |

---

## 五、验证步骤

### Phase 1 验证（物模型 + 设备认证）
1. 创建产品 → 添加物模型属性（温度/湿度/开关）→ 注册设备关联产品
2. 设备上报含多属性的 JSON payload → TDengine device_properties 超表正确存储所有属性
3. DeviceSimulator 按物模型生成数据（验证日志）
4. EMQX 禁用匿名后，无凭据连接被拒绝
5. 能碳分析 API 返回动态设备档案（非硬编码）
6. 前端产品列表 + 物模型编辑器可操作
7. 设备详情页展示物模型 Tab
8. iot 模块 PostgreSQL 连接正常（密码已修复）

### Phase 2 验证（设备影子 + 指令下发）
9. 前端设置设备影子 desired（如开关=true）→ MQTT 下发命令 → 设备响应 → reported 更新
10. 设备离线时设置 desired → 设备上线后同步
11. 设备详情页影子 Tab 展示 desired/reported 双列
12. 设备控制 Tab 调用物模型服务
13. 指令历史 API 返回指令记录

### Phase 3 验证（设备管理增强）
14. 创建分组树 → 移动设备到分组 → 按分组筛选设备列表
15. 设备日志 API 返回真实日志记录
16. 停止设备模拟器 → 1 分钟后设备自动标记 OFFLINE → 日志记录状态变更
17. 上传固件 → 下载接口返回预签名 URL → 创建 OTA 任务 → 设备收到 MQTT 升级命令 → 版本回写
18. 批量导入 Excel → 设备列表显示新设备

### Phase 4 验证（规则引擎增强 + 多渠道告警）
19. 创建场景联动：设备 A 温度 >30℃ → 下发指令关闭设备 B
20. 创建定时场景：每天 22:00 → 所有设备开关=false
21. 创建 Webhook 通知渠道 → 触发告警 → 外部 URL 收到 POST
22. 创建邮件通知渠道 → 触发告警 → 收到邮件

### Phase 5 验证（可视化大屏 + 前端增强）
23. 设备地图显示所有设备标记点，颜色区分状态
24. 设备拓扑图显示网关-子设备树形结构
25. 实时大屏 MQTT 数据刷新 + 告警滚动
26. DeviceDetail 历史数据 Tab 实时更新
27. 无权限路由访问被拦截

### 最终整体验证
28. `docker compose up -d --build` 全量重建
29. 全链路验证：创建产品→定义物模型→注册设备→设备上报→数据解析→场景触发→指令下发→影子同步→前端展示→告警通知→可视化大屏

---

## 六、文件变更清单汇总

### Phase 1 新建文件（15 个）
- device: Product.java, ThingModel.java, ThingService.java, ProductMapper.java, ThingModelMapper.java, ThingServiceMapper.java, ProductService.java, ProductController.java
- iot: ThingModelCacheService.java
- 前端: product.ts, ProductList.vue, ProductDetail.vue, ThingModelPropertyEditor.vue, ThingModelServiceEditor.vue

### Phase 1 修改文件（10 个）
- device: Device.java, DeviceRegisterRequest.java
- iot: MqttSubscriber.java, TimeSeriesService.java, DeviceSimulator.java, EmqxClientConfig.java, application-docker.yml
- gateway: application-docker.yml
- analytics: EnergyAnalyticsService.java, application-docker.yml
- docker: init-databases.sh, docker-compose.yml
- 前端: router/index.ts, DeviceList.vue, DeviceDetail.vue

### Phase 2 新建文件（8 个）
- iot: DeviceShadow.java, DeviceShadowService.java, DeviceCommandService.java, DeviceCommandController.java
- 前端: device-control.ts, DeviceShadow.vue, DeviceControl.vue

### Phase 2 修改文件（2 个）
- iot: MqttSubscriber.java
- 前端: DeviceDetail.vue

### Phase 3 新建文件（10 个）
- device: DeviceGroupService.java, DeviceGroupController.java, DeviceLogService.java, DeviceLogController.java, DeviceStatusCheckJob.java, DeviceBatchController.java, DeviceBatchImportService.java
- ota: DeviceIntegrationService.java, MqttClientConfig.java
- 前端: device-group.ts, DeviceGroupTree.vue, DeviceImport.vue

### Phase 3 修改文件（8 个）
- device: DeviceApplication.java, pom.xml, application-docker.yml
- ota: FirmwareController.java, FirmwareService.java, OtaTaskService.java, pom.xml, application-docker.yml
- 前端: DeviceList.vue, FirmwareList.vue, OtaTaskList.vue, package.json

### Phase 4 新建文件（9 个）
- iot: Scene.java, SceneAction.java, SceneLinkageService.java, SceneController.java, SceneTimerScheduler.java
- device: NotificationService.java, NotificationController.java
- 前端: scene.ts, notification.ts, SceneList.vue, SceneEditor.vue, NotificationConfig.vue

### Phase 4 修改文件（4 个）
- iot: MqttSubscriber.java, IotApplication.java
- device: AlertManageService.java, pom.xml, application-docker.yml
- 前端: router/index.ts

### Phase 5 新建文件（3 个）
- 前端: DeviceMap.vue, DeviceTopology.vue, RealtimeDashboard.vue

### Phase 5 修改文件（5 个）
- device: Device.java, DeviceController.java, DeviceService.java
- docker: init-databases.sh
- 前端: DeviceDetail.vue, router/index.ts, MainLayout.vue, package.json

---

## 七、执行顺序与依赖

```
Phase 1（物模型 + 认证）← 必须最先完成
   ↓ 依赖：物模型属性定义
Phase 2（影子 + 指令）← 依赖 Phase 1 的物模型服务定义
   ↓ 依赖：指令下发能力
Phase 3（设备管理增强）← OTA 闭环依赖 Phase 2 的 MQTT 下发
   ↓ 依赖：无（可与 Phase 4 并行，但建议先完成）
Phase 4（场景联动）← 依赖 Phase 1 物模型 + Phase 2 指令下发
   ↓ 依赖：无
Phase 5（可视化大屏）← 依赖前面所有 Phase 的数据
```

**严格按序执行，每个 Phase 完成后独立验证，通过后再进入下一个 Phase。**
