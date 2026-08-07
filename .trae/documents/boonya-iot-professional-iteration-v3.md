# Boonya IoT 专业能力迭代计划 v3

> 范围：核心 IoT 抽象层 + 设备管理增强 + 规则引擎增强 + 可视化大屏
> 深度：全链路贯通（后端 API + 前端页面可操作，端到端流程走通）
> 原则：功能严格衔接，物模型→数据解析→规则引擎→指令下发→设备影子 形成闭环

---

## 一、当前状态分析

### 已有基础（v2 迭代成果）
- 20 个 Docker 容器运行，核心链路 MQTT→TDengine→规则引擎→告警→前端 全通
- 告警闭环：持久化 + 状态流转 + 历史查询
- RBAC 权限：sys_permission + 网关授权 + 前端动态路由
- 前端 13 个页面均有真实业务逻辑
- UI 已改版为 IoT 科技深蓝配色

### 专业 IoT 能力差距（15 项）
| # | 差距 | 影响 |
|---|------|------|
| 1 | 物模型完全缺失 | 数据解析硬编码，无法泛化接入新设备类型 |
| 2 | 设备影子缺失 | 离线设备无法控制，期望/上报状态无法同步 |
| 3 | 设备认证形同虚设 | authToken 生成后无验证，MQTT 匿名访问 |
| 4 | 设备分组无 API | 实体存在但无 Controller/Service |
| 5 | 无离线检测定时任务 | 设备断连后状态永远停留在 ONLINE |
| 6 | OTA 固件下载接口缺失 | 设备端无法获取固件二进制，流程不闭环 |
| 7 | OTA 版本不回写 | 升级成功后 Device.firmwareVersion 不更新 |
| 8 | 无批量设备导入 | 仅支持单个注册 |
| 9 | 场景联动缺失 | 规则仅单设备阈值告警，无跨设备联动 |
| 10 | 定时规则缺失 | 无 cron 触发能力 |
| 11 | 设备指令下发缺失 | 无法通过 MQTT 向设备下发控制命令 |
| 12 | 告警通知渠道单一 | 仅 WebSocket/MQTT，无邮件/Webhook |
| 13 | 无 GIS 地图 | 无设备地理分布可视化 |
| 14 | 无设备拓扑图 | 无网关-子设备关系可视化 |
| 15 | MQTT LWT/Retained 缺失 | 设备异常断连无通知，上线无法获取最后状态 |

---

## 二、实施计划（5 个 Phase，严格按序执行）

### Phase 1：物模型体系（IoT 平台根基）

> 物模型是后续数据解析、规则引擎、指令下发的数据基础，必须最先完成。

#### 1.1 后端：物模型数据模型 + API（device 模块）

**新建表**（schema.sql 追加）：
```sql
-- 产品（设备模板）
CREATE TABLE IF NOT EXISTS iot_product (
    id BIGSERIAL PRIMARY KEY,
    product_key VARCHAR(64) UNIQUE NOT NULL,   -- 产品唯一标识
    product_name VARCHAR(128) NOT NULL,
    node_type VARCHAR(16) DEFAULT 'DIRECT',     -- DIRECT(直连)/GATEWAY(网关)/SUBDEVICE(子设备)
    protocol_type VARCHAR(16) DEFAULT 'MQTT',
    data_format VARCHAR(16) DEFAULT 'JSON',     -- JSON/CUSTOM
    description TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 物模型属性
CREATE TABLE IF NOT EXISTS iot_thing_model (
    id BIGSERIAL PRIMARY KEY,
    product_key VARCHAR(64) NOT NULL,
    identifier VARCHAR(64) NOT NULL,            -- 属性标识 (temperature/humidity/switch等)
    name VARCHAR(128) NOT NULL,                 -- 属性名称
    data_type VARCHAR(16) NOT NULL,             -- int/float/double/string/bool/enum
    unit VARCHAR(32),                           -- 单位 (°C/%/kW等)
    min_value DOUBLE PRECISION,
    max_value DOUBLE PRECISION,
    access_mode VARCHAR(16) DEFAULT 'RW',       -- R(只读)/W(只写)/RW(读写)
    description TEXT,
    sort INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_key, identifier)
);

-- 物模型服务（设备可被调用的功能）
CREATE TABLE IF NOT EXISTS iot_thing_service (
    id BIGSERIAL PRIMARY KEY,
    product_key VARCHAR(64) NOT NULL,
    identifier VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    call_type VARCHAR(16) DEFAULT 'ASYNC',      -- SYNC/ASYNC
    input_params TEXT,                           -- JSON 格式输入参数定义
    output_params TEXT,                          -- JSON 格式输出参数定义
    description TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_key, identifier)
);
```

**新建文件**：
- `device/entity/Product.java` — 产品实体
- `device/entity/ThingModel.java` — 物模型属性实体
- `device/entity/ThingService.java` — 物模型服务实体
- `device/mapper/ProductMapper.java`
- `device/mapper/ThingModelMapper.java`
- `device/mapper/ThingServiceMapper.java`
- `device/service/ProductService.java` — 产品 CRUD + 物模型管理
- `device/controller/ProductController.java` — 产品 + 物模型 API
  ```
  GET    /api/products                    — 产品列表
  POST   /api/products                    — 创建产品
  GET    /api/products/{productKey}       — 产品详情
  PUT    /api/products/{productKey}       — 更新产品
  DELETE /api/products/{productKey}       — 删除产品
  GET    /api/products/{productKey}/properties  — 物模型属性列表
  POST   /api/products/{productKey}/properties  — 添加属性
  PUT    /api/thing-model/properties/{id}       — 更新属性
  DELETE /api/thing-model/properties/{id}       — 删除属性
  GET    /api/products/{productKey}/services    — 物模型服务列表
  POST   /api/products/{productKey}/services    — 添加服务
  ```

**修改文件**：
- `device/entity/Device.java` — 添加 `productKey` 字段（关联产品）
- `device/schema.sql` — Device 表添加 `product_key VARCHAR(64)` 列

#### 1.2 后端：基于物模型的数据解析（iot 模块）

**修改文件**：`iot/service/MqttSubscriber.java`
- 设备上报数据时，先查询设备的 `productKey`
- 通过 `productKey` 查询物模型属性定义
- 根据物模型动态解析 payload 中的属性字段（替代硬编码 `json.get("temp")`）
- 遍历物模型属性列表，将每个属性值写入 TDengine

**修改文件**：`iot/service/TimeSeriesService.java`
- `saveDeviceData` 方法扩展：支持写入多个属性（温度/湿度/开关等），而非仅温度
- TDengine 超表 schema 扩展：根据物模型动态建表

**新建文件**：`iot/service/ThingModelService.java`
- `getThingModel(productKey)` — 缓存物模型到 Redis（TTL 5 分钟）
- `parsePayload(productKey, payload)` — 根据物模型解析 JSON payload 为属性 Map
- `getProperties(productKey)` — 获取属性列表

#### 1.3 后端：网关路由 + 设备认证

**修改文件**：`gateway/application-docker.yml`
- 添加 `/api/products/**` 路由到 device 服务
- 添加 `/api/thing-model/**` 路由到 device 服务

**修改文件**：`iot/config/EmqxClientConfig.java`
- `MqttConnectOptions` 添加 `setUserName()`/`setPassword()`（使用配置的凭据）
- 添加 LWT 遗嘱消息：设备断连时发布 `device/{deviceId}/offline`

**修改文件**：`iot/mqtt/MqttClientWrapper.java`
- `subscribe` 方法添加 QoS 参数

#### 1.4 前端：物模型管理页面

**新建文件**：
- `admin/src/api/product.ts` — 产品 + 物模型 API
- `admin/src/views/device/ProductList.vue` — 产品列表页（CRUD + 查看物模型）
- `admin/src/views/device/ThingModelEditor.vue` — 物模型编辑器（属性列表 + 服务列表 Tab）
- `admin/src/views/device/ProductDetail.vue` — 产品详情（基本信息 + 物模型属性 + 物模型服务）

**修改文件**：
- `admin/src/router/index.ts` — 添加 `/products` 和 `/products/:productKey` 路由
- `admin/src/views/devices/DeviceList.vue` — 注册设备时添加产品选择下拉框
- `admin/src/views/devices/DeviceDetail.vue` — 添加「物模型」Tab，展示设备的属性/服务/事件

---

### Phase 2：设备影子 + 指令下发（设备控制闭环）

> 物模型定义了设备的"读写"能力，设备影子实现"写"的落地，指令下发实现"写"的传输。

#### 2.1 后端：设备影子（iot 模块）

**新建表**（schema-postgres.sql 追加）：
```sql
CREATE TABLE IF NOT EXISTS device_shadow (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(64) NOT NULL UNIQUE,
    desired_state TEXT,          -- 期望状态 JSON
    reported_state TEXT,         -- 上报状态 JSON
    version BIGINT DEFAULT 0,    -- 影子版本号
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
  - `updateDesired(deviceId, properties)` — 更新期望状态（前端/规则引擎调用）
  - `updateReported(deviceId, properties)` — 更新上报状态（设备数据上报时自动调用）
  - `clearDesired(deviceId)` — 清除期望状态（设备同步后）
  - 影子变更后通过 MQTT 发布到 `device/{deviceId}/shadow`

**修改文件**：
- `iot/service/MqttSubscriber.java` — 设备上报数据时，同步更新影子的 reported_state

#### 2.2 后端：设备指令下发（iot 模块）

**新建文件**：
- `iot/service/DeviceCommandService.java`
  - `sendCommand(deviceId, commandId, serviceIdentifier, params)` — 通过 MQTT 下发服务调用指令
  - 发布到 `device/{deviceId}/command` 主题，payload 含 commandId + serviceId + params
  - 记录指令到 DB（指令表）

**新建表**：
```sql
CREATE TABLE IF NOT EXISTS device_command (
    id BIGSERIAL PRIMARY KEY,
    command_id VARCHAR(64) NOT NULL,
    device_id VARCHAR(64) NOT NULL,
    service_identifier VARCHAR(64),
    params TEXT,                  -- JSON 参数
    status VARCHAR(16) DEFAULT 'PENDING',  -- PENDING/SENT/SUCCESS/FAILED/TIMEOUT
    result TEXT,                  -- JSON 执行结果
    sent_time TIMESTAMP,
    ack_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**新建文件**：
- `iot/controller/DeviceCommandController.java`
  ```
  POST /api/iot/devices/{deviceId}/command   — 下发指令
  GET  /api/iot/devices/{deviceId}/commands  — 指令历史
  GET  /api/iot/devices/{deviceId}/shadow    — 获取设备影子
  PUT  /api/iot/devices/{deviceId}/shadow    — 更新期望状态
  ```

**修改文件**：
- `iot/service/MqttSubscriber.java` — 订阅 `device/+/command/resp` 主题，接收设备指令响应

#### 2.3 前端：设备影子 + 指令下发页面

**新建文件**：
- `admin/src/api/device-control.ts` — 设备影子 + 指令 API
- `admin/src/views/devices/DeviceShadow.vue` — 设备影子面板（desired/reported 双列对比 + 同步状态指示）
  - 左列：期望状态（可编辑，属性值输入框）
  - 右列：上报状态（只读，实时更新）
  - 底部：版本号 + 最后更新时间 + 同步按钮

**修改文件**：
- `admin/src/views/devices/DeviceDetail.vue` — 添加「设备影子」Tab 和「设备控制」Tab
  - 设备控制 Tab：列出物模型中的可写属性 + 可调用服务，提供属性设置和服务调用 UI

---

### Phase 3：设备管理增强（运营效率提升）

#### 3.1 后端：设备分组 CRUD（device 模块）

**新建文件**：
- `device/controller/DeviceGroupController.java`
  ```
  GET    /api/device-groups              — 分组树
  POST   /api/device-groups              — 创建分组
  PUT    /api/device-groups/{id}         — 更新分组
  DELETE /api/device-groups/{id}         — 删除分组
  PUT    /api/devices/{deviceId}/group   — 设备移动到分组
  GET    /api/device-groups/{id}/devices — 分组下设备列表
  ```
- `device/service/DeviceGroupService.java`

#### 3.2 后端：离线检测定时任务（device 模块）

**新建文件**：
- `device/job/DeviceStatusCheckJob.java`
  - `@Scheduled(fixedRate = 60000)` — 每分钟扫描
  - 检查 Redis 中设备心跳 TTL，超时设备标记 OFFLINE
  - 设备状态变更时发布 MQTT 通知 `device/{deviceId}/status`

#### 3.3 后端：OTA 闭环补齐（ota 模块）

**修改文件**：
- `ota/controller/FirmwareController.java` — 添加下载接口
  ```
  GET /api/firmware/{id}/download  — 生成预签名下载 URL 或直接流式下载
  ```
- `ota/service/OtaTaskService.java`
  - `createOtaTask` 成功后，通过 MQTT 发布升级命令到 `device/{deviceId}/ota/upgrade`
  - `updateTaskStatus` 成功时，通过 HTTP 调用 device 服务更新 `Device.firmwareVersion`

**新建文件**：
- `ota/service/DeviceIntegrationService.java` — 调用 device 服务 API 同步固件版本

**修改文件**：
- `iot/service/MqttSubscriber.java` — 订阅 `device/+/ota/progress` 接收设备升级进度上报

#### 3.4 后端：批量设备导入（device 模块）

**新建文件**：
- `device/controller/DeviceBatchController.java`
  ```
  POST /api/devices/batch-import   — Excel/CSV 批量导入
  GET  /api/devices/import-template — 下载导入模板
  ```
- `device/service/DeviceBatchImportService.java` — 解析 Excel，批量注册设备

#### 3.5 前端：设备管理增强页面

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

---

### Phase 4：规则引擎增强 + 多渠道告警

> 依赖 Phase 1 的物模型（属性定义）和 Phase 2 的指令下发（COMMAND 动作）。

#### 4.1 后端：场景联动引擎（iot 模块）

**新建表**：
```sql
CREATE TABLE IF NOT EXISTS scene_linkage (
    id BIGSERIAL PRIMARY KEY,
    scene_id VARCHAR(64) NOT NULL UNIQUE,
    scene_name VARCHAR(128) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    trigger_type VARCHAR(16) NOT NULL,        -- DEVICE/DATA/TIMER/MANUAL
    trigger_config TEXT,                       -- JSON: 触发条件配置
    condition_config TEXT,                     -- JSON: 过滤条件
    action_config TEXT,                        -- JSON: 动作列表
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
  - `evaluate(deviceId, data)` — 设备数据上报时评估场景触发条件
  - `executeActions(scene, deviceId, data)` — 执行动作列表
    - ALERT: 创建告警
    - COMMAND: 调用 DeviceCommandService 下发指令
    - WEBHOOK: HTTP POST 到外部 URL
    - SHADOW_UPDATE: 更新设备影子期望状态
  - `executeTimer(scene)` — 定时触发
- `iot/controller/SceneController.java`
  ```
  GET    /api/scenes            — 场景列表
  POST   /api/scenes            — 创建场景
  PUT    /api/scenes/{id}       — 更新场景
  DELETE /api/scenes/{id}       — 删除场景
  PUT    /api/scenes/{id}/toggle — 启用/禁用
  POST   /api/scenes/{id}/execute — 手动触发
  ```

#### 4.2 后端：定时规则调度（iot 模块）

**新建文件**：
- `iot/job/SceneTimerScheduler.java`
  - 使用 Spring `@Scheduled` 或 `TaskScheduler` 
  - 启动时从 DB 加载 enabled 且 trigger_type=TIMER 的场景
  - 按 cron 表达式调度执行
  - 场景增删改时动态更新调度

#### 4.3 后端：多渠道告警通知（device 模块）

**新建表**：
```sql
CREATE TABLE IF NOT EXISTS alert_notification_channel (
    id BIGSERIAL PRIMARY KEY,
    channel_name VARCHAR(128) NOT NULL,
    channel_type VARCHAR(16) NOT NULL,     -- EMAIL/WEBHOOK/DINGTALK/FEISHU
    config TEXT,                            -- JSON: 渠道配置
    enabled BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS alert_notification_rule (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT,                         -- 关联告警规则
    channel_id BIGINT,                      -- 关联通知渠道
    severity_filter VARCHAR(16),            -- 仅通知某级别以上
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
  GET    /api/notification/channels    — 渠道列表
  POST   /api/notification/channels    — 创建渠道
  PUT    /api/notification/channels/{id} — 更新渠道
  DELETE /api/notification/channels/{id} — 删除渠道
  POST   /api/notification/channels/{id}/test — 测试发送
  ```

**修改文件**：
- `device/service/AlertManageService.java` — `createAlert` 后调用 `NotificationService`

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
- `admin/src/router/index.ts` — 添加 `/automation/scenes` 和 `/system/notifications` 路由

---

### Phase 5：可视化大屏（GIS 地图 + 设备拓扑 + 实时大屏）

#### 5.1 后端：设备位置 + 拓扑数据 API

**修改文件**：
- `device/entity/Device.java` — 添加 `longitude`(经度) / `latitude`(纬度) 字段
- `device/schema.sql` — Device 表添加 longitude/latitude 列
- `device/controller/DeviceController.java`
  ```
  GET /api/devices/map  — 返回所有设备的位置+状态+最新数据
  GET /api/devices/topology  — 返回网关-子设备拓扑结构
  ```

**修改文件**：
- `iot/service/MqttSubscriber.java` — 网关设备上报时，解析子设备数据

#### 5.2 前端：GIS 地图大屏

**新建文件**：
- `admin/src/views/visualization/DeviceMap.vue` — 设备地图大屏
  - 使用高德地图 JS API（或 Leaflet 开源方案）
  - 设备标记点：颜色区分在线/离线/告警
  - 点击标记弹出设备信息卡片
  - 左侧统计面板：总数/在线/离线/告警
  - 支持地图缩放和拖拽

#### 5.3 前端：设备拓扑图

**新建文件**：
- `admin/src/views/visualization/DeviceTopology.vue` — 设备拓扑可视化
  - 使用 ECharts Tree/Graph 图
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

**修改文件**：
- `admin/src/router/index.ts` — 添加 `/visualization/map`、`/visualization/topology`、`/visualization/dashboard` 路由
- `admin/src/layouts/MainLayout.vue` — 菜单添加「可视化大屏」分组

---

## 三、功能衔接关系图

```
物模型(Phase 1)
  ├── 定义设备属性/服务/事件
  ├── → 数据解析: MqttSubscriber 根据物模型动态解析 payload
  ├── → 设备详情: 前端展示物模型属性
  ├── → 规则引擎: 条件基于物模型属性
  └── → 指令下发: 调用物模型定义的服务

设备影子(Phase 2)
  ├── → 设备控制: 前端设置 desired → MQTT 下发 → 设备执行 → reported 回传
  ├── → 离线控制: 设备离线时 desired 缓存，上线后同步
  └── → 场景联动: SHADOW_UPDATE 动作更新 desired

指令下发(Phase 2)
  ├── → 设备控制: 调用物模型服务 → MQTT 发布命令
  ├── → OTA 触发: 升级命令通过 MQTT 下发
  └── → 场景联动: COMMAND 动作下发指令

场景联动(Phase 4)
  ├── 触发: 设备数据(物模型属性) / 定时 / 手动
  ├── 条件: 属性阈值 / 时间范围
  └── 动作: 告警 / 指令下发 / Webhook / 影子更新

可视化(Phase 5)
  ├── GIS 地图: 设备位置 + 状态 + 告警
  ├── 拓扑图: 网关-子设备关系
  └── 实时大屏: MQTT 实时数据 + 告警滚动
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
| 6 | GIS 地图方案 | 高德地图 JS API | 国内环境友好，免费额度充足 |
| 7 | 设备拓扑图 | ECharts Graph | 无需额外依赖，项目已有 ECharts |
| 8 | 批量导入格式 | Excel(.xlsx) | 企业用户习惯，Apache POI 解析 |
| 9 | OTA 版本回写方式 | HTTP 调用 device 服务 | 避免跨模块 DB 直连，保持服务边界 |
| 10 | 告警通知渠道优先级 | Email + Webhook + 钉钉 | 覆盖企业最常用渠道 |

---

## 五、验证步骤

### Phase 1 验证（物模型）
1. 创建产品 → 添加物模型属性（温度/湿度/开关）→ 注册设备关联产品
2. 设备上报含多属性的 JSON payload → TDengine 正确存储所有属性
3. 前端产品列表 + 物模型编辑器可操作
4. 设备详情页展示物模型 Tab

### Phase 2 验证（设备影子 + 指令下发）
5. 前端设置设备影子 desired（如开关=true）→ MQTT 下发命令 → 设备响应 → reported 更新
6. 设备离线时设置 desired → 设备上线后同步
7. 设备详情页影子 Tab 展示 desired/reported 双列
8. 设备控制 Tab 调用物模型服务

### Phase 3 验证（设备管理增强）
9. 创建分组树 → 移动设备到分组 → 按分组筛选设备列表
10. 停止设备模拟器 → 1 分钟后设备自动标记 OFFLINE
11. 上传固件 → 下载接口可获取 → 创建 OTA 任务 → 设备收到 MQTT 升级命令 → 版本回写
12. 批量导入 Excel → 设备列表显示新设备

### Phase 4 验证（规则引擎增强 + 多渠道告警）
13. 创建场景联动：设备 A 温度 >30℃ → 下发指令关闭设备 B
14. 创建定时场景：每天 22:00 → 所有设备开关=false
15. 创建 Webhook 通知渠道 → 触发告警 → 外部 URL 收到 POST
16. 创建邮件通知渠道 → 触发告警 → 收到邮件

### Phase 5 验证（可视化大屏）
17. 设备地图显示所有设备标记点，颜色区分状态
18. 设备拓扑图显示网关-子设备树形结构
19. 实时大屏 MQTT 数据刷新 + 告警滚动

### 最终整体验证
20. `docker compose up -d --build` 全量重建
21. 全链路验证：创建产品→定义物模型→注册设备→设备上报→数据解析→规则触发→场景联动→指令下发→影子同步→前端展示
