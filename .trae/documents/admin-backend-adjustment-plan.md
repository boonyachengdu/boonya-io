# Boonya IoT Admin 后台接口与页面调整计划

> 生成日期：2026-08-06
> 范围：boonya-io-frontend/admin 前端 + boonya-io-{device,analytics,ota,auth} 后端模块

---

## 一、现状分析（Repository Research Conclusion）

### 1.1 模块路由结构（前端）

菜单路由定义在 [router/index.ts](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/router/index.ts#L4-L53)，共 6 个功能模块：

| 路径 | 页面组件 | 菜单名称 | 后端服务 | 状态评估 |
|------|----------|----------|----------|----------|
| /dashboard | [Dashboard.vue](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/views/Dashboard.vue) | 数据看板 | analytics | ⚠️ 部分可用（设备下拉硬编码） |
| /devices | [DeviceList.vue](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/views/devices/DeviceList.vue) | 设备管理 | device | ⚠️ 结构完整但需校验修复 |
| /firmware | [FirmwareList.vue](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/views/ota/FirmwareList.vue) | 固件管理 | ota | ⚠️ 缺少分页 |
| /ota-tasks | [OtaTaskList.vue](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/views/ota/OtaTaskList.vue) | OTA任务 | ota | ❌ 仅支持按设备ID查询，无全局视图 |
| /analytics | [DataAnalytics.vue](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/views/analytics/DataAnalytics.vue) | 数据分析 | analytics | ✅ 用户确认基本完整 |
| /energy | [EnergyDashboard.vue](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/views/energy/EnergyDashboard.vue) | 能碳管理 | analytics | ⚠️ 后端返回Map无强类型 |

### 1.2 后端接口完整度评估

#### ✅ Auth 模块 [AuthController.java](file:///e:/AI/java/boonya-io/boonya-io-auth/src/main/java/com/boonya/lab/io/auth/controller/AuthController.java)
- 登录 login() / 登出 logout() / 刷新 refreshToken() / 注册 registerUser()
- **返回格式：Result<T>**

#### ⚠️ Device 模块 [DeviceController.java](file:///e:/AI/java/boonya-io/boonya-io-device/src/main/java/com/boonya/lab/io/device/controller/DeviceController.java)
接口全，但有 2 个问题：
1. **响应格式混用 ResponseEntity<T>**，其他模块用 Result<T>，前端虽然兼容但不统一
2. **PUT /{id} 更新状态实现路径迂回**：按主键id查Device → 取deviceId → 再按deviceId查 → 再更新，可简化直接按主键更新

#### ✅ Analytics Dashboard 模块 [DashboardController.java](file:///e:/AI/java/boonya-io/boonya-io-analytics/src/main/java/com/boonya/lab/io/analytics/controller/DashboardController.java)
- overview / device/{deviceId}/realtime / device/{deviceId}/trend
- **返回格式：Result<T>**，部分返回 Map<String,Object>

#### ⚠️ Analytics Energy 模块 [EnergyAnalyticsController.java](file:///e:/AI/java/boonya-io/boonya-io-analytics/src/main/java/com/boonya/lab/io/analytics/controller/EnergyAnalyticsController.java)
5 个接口齐全，但**全部返回 List<Map<String,Object>> / Map<String,Object> 弱类型**，前端字段映射有风险

#### ⚠️ OTA Firmware 模块 [FirmwareController.java](file:///e:/AI/java/boonya-io/boonya-io-ota/src/main/java/com/boonya/lab/io/ota/controller/FirmwareController.java)
- upload / list / get / publish / archive / delete
- **listFirmwares 返回 List<Firmware> 无分页**，数据量大时卡死

#### ❌ OTA Task 模块 [OtaTaskController.java](file:///e:/AI/java/boonya-io/boonya-io-ota/src/main/java/com/boonya/lab/io/ota/controller/OtaTaskController.java)
- 缺少**全局分页查询接口**（只有按deviceId查），管理员无法看到全部OTA任务总览

### 1.3 前端 API 层（src/api/）

| 文件 | 状态 | 问题 |
|------|------|------|
| [auth.ts](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/api/auth.ts) | ✅ 完整 | - |
| [analytics.ts](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/api/analytics.ts) | ✅ 完整 | （参考基准） |
| [device.ts](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/api/device.ts) | ⚠️ | PageResult字段需对齐 MyBatis-Plus Page |
| [energy.ts](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/api/energy.ts) | ⚠️ | 接口强类型定义 vs 后端 Map 返回可能错位 |
| [firmware.ts](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/api/firmware.ts) | ⚠️ | list 无分页支持 |
| [ota.ts](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/api/ota.ts) | ❌ | 缺全局分页查询接口定义 |

### 1.4 网关路由 [application-docker.yml](file:///e:/AI/java/boonya-io/boonya-io-gateway/src/main/resources/application-docker.yml#L21-L98)
✅ 6 个服务路由全部配置，无新增路由需求

---

## 二、需要修改的文件与模块清单

### 2.1 后端 Java 模块（共 4 个模块、10 个文件）

| 模块 | 文件 | 操作类型 |
|------|------|----------|
| boonya-io-device | DeviceController.java | 修改：统一Result格式、简化update逻辑 |
| boonya-io-device | DeviceService.java | 修改：新增按主键直接更新状态方法 |
| boonya-io-analytics | EnergyAnalyticsController.java | 修改：弱类型Map → 强类型DTO |
| boonya-io-analytics | EnergyAnalyticsService.java | 修改：返回DTO而非Map |
| boonya-io-analytics | **新增** EnergyOverview DTO等 | 新增：5个强类型DTO类 |
| boonya-io-ota | FirmwareController.java | 修改：list接口加分页 |
| boonya-io-ota | FirmwareService.java | 修改：分页查询 |
| boonya-io-ota | OtaTaskController.java | 修改：新增全局分页查询接口 |
| boonya-io-ota | OtaTaskService.java | 修改：新增分页查询方法 |
| boonya-io-common（如有） | Result工具 | 确认：供device模块引用 |

### 2.2 前端 Vue/TS 模块（共 6 个页面、3 个 API 文件）

| 文件 | 操作类型 |
|------|----------|
| api/device.ts | 修改：对齐PageResult字段 |
| api/firmware.ts | 修改：增加分页参数+返回 |
| api/ota.ts | 修改：增加全局分页查询API |
| views/Dashboard.vue | 修改：设备下拉从后端动态拉取 |
| views/devices/DeviceList.vue | 修改：对齐分页字段+移除可能的bug |
| views/ota/FirmwareList.vue | 修改：增加分页组件 |
| views/ota/OtaTaskList.vue | 修改：增加全局列表视图+分页 |
| views/energy/EnergyDashboard.vue | 校验：对齐新DTO字段名 |
| utils/request.ts | 保留现有双格式兼容，后续可逐步移除 |

---

## 三、具体调整步骤

### 阶段一：后端接口标准化（先做后端，前端再对齐）

#### Step 1.1 统一 DeviceController 返回格式
- **目标**：ResponseEntity<T> → Result<T>，与其他模块保持一致
- **涉及文件**：
  - [DeviceController.java](file:///e:/AI/java/boonya-io/boonya-io-device/src/main/java/com/boonya/lab/io/device/controller/DeviceController.java)
- **修改内容**：
  - 所有方法返回类型从 `ResponseEntity<X>` 改为 `Result<X>`
  - `ResponseEntity.ok(response)` → `Result.success(response)`
  - `ResponseEntity.ok().build()` → `Result.success()` / `Result.success("操作成功", null)`
  - 注入 `com.boonya.lab.io.common.response.Result` 包
- **风险**：前端 request.ts 已兼容两种格式，短期不会断，但建议同步改完

#### Step 1.2 简化 Device 更新状态逻辑（减少一次无效查询）
- **目标**：PUT /{id}?status= 直接按主键id更新，不再绕 deviceId
- **涉及文件**：
  - [DeviceService.java](file:///e:/AI/java/boonya-io/boonya-io-device/src/main/java/com/boonya/lab/io/device/service/DeviceService.java)
  - DeviceController.java
- **修改内容**：
  - DeviceService 新增 `updateDeviceStatusById(Long id, String status)`：直接 selectById + updateById
  - Controller 中的 updateDeviceStatus 调用新方法
  - 保留原有 `updateDeviceStatus(String deviceId, String status)` 供设备端心跳使用

#### Step 1.3 EnergyAnalytics 强类型 DTO 化
- **目标**：消灭 5 个接口的 Map<String,Object> 弱类型返回
- **涉及文件**：
  - EnergyAnalyticsController.java
  - [EnergyAnalyticsService.java](file:///e:/AI/java/boonya-io/boonya-io-analytics/src/main/java/com/boonya/lab/io/analytics/service/EnergyAnalyticsService.java)
  - **新增**：dto/ 目录下 5 个 DTO
- **修改内容**：
  - 新增 DTO：EnergyOverview、EnergyTrendPoint、AreaRankingItem、EnergyDeviceStatus、EnergyAlarm（字段对齐前端 energy.ts 接口定义）
  - Service 层方法返回类型从 Map/List<Map> → DTO/List<DTO>
  - Controller 方法签名同步更新
- **DTO 字段规范**：
  - EnergyOverview：siteName, period, electricityKwh, waterM3, solarKwh, storageDischargeKwh, energyCostCny, carbonTons, carbonReductionTons, activeAlarms, onlineDevices, totalDevices
  - EnergyTrendPoint：time, electricityKwh, waterM3, solarKwh, carbonTons
  - AreaRankingItem：name, electricityKwh, waterM3, carbonTons, trend
  - EnergyDeviceStatus：deviceId, deviceName, deviceType, status(enum:online/offline/warning), value, unit, location
  - EnergyAlarm：level(high/medium/low), title, deviceId, description, status, time

#### Step 1.4 FirmwareController 增加分页
- **目标**：GET /api/firmware 支持 pageNum/pageSize 分页
- **涉及文件**：
  - [FirmwareController.java](file:///e:/AI/java/boonya-io/boonya-io-ota/src/main/java/com/boonya/lab/io/ota/controller/FirmwareController.java)
  - [FirmwareService.java](file:///e:/AI/java/boonya-io/boonya-io-ota/src/main/java/com/boonya/lab/io/ota/service/FirmwareService.java)
- **修改内容**：
  - listFirmwares 参数增加 `@RequestParam(defaultValue="1") int pageNum, @RequestParam(defaultValue="20") int pageSize`
  - Service 层改用 MyBatis-Plus Page<Firmware> 分页查询
  - Controller 返回 `Result<Page<Firmware>>`
  - 保留原有 deviceModel、status 筛选条件

#### Step 1.5 OtaTaskController 新增全局分页查询
- **目标**：管理员可以不输入 deviceId 直接看到所有 OTA 任务
- **涉及文件**：
  - [OtaTaskController.java](file:///e:/AI/java/boonya-io/boonya-io-ota/src/main/java/com/boonya/lab/io/ota/controller/OtaTaskController.java)
  - [OtaTaskService.java](file:///e:/AI/java/boonya-io/boonya-io-ota/src/main/java/com/boonya/lab/io/ota/service/OtaTaskService.java)
- **修改内容**：
  - 新增接口：`GET /api/ota/tasks?pageNum=1&pageSize=20&deviceId=&status=`
    - deviceId 可选（空=查全部），status 可选，按 createTime 倒序
  - Service 新增 `Page<OtaTask> queryTasks(int pageNum, int pageSize, String deviceId, String status)`
  - 返回格式 `Result<Page<OtaTask>>`
  - 保留原 GET /device/{deviceId} 单设备查询（兼容旧前端）

---

### 阶段二：前端 API 层 + 页面调整

#### Step 2.1 API 层对齐后端
- **涉及文件**：
  - [device.ts](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/api/device.ts)
  - [firmware.ts](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/api/firmware.ts)
  - [ota.ts](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/api/ota.ts)
- **修改内容**：
  - **device.ts**：对齐 MyBatis-Plus Page 字段（records / total / current / size / pages），当前基本正确
  - **firmware.ts**：getFirmwareList 增加 pageNum、pageSize 参数；返回类型改为 `PageResult<Firmware>`
  - **ota.ts**：新增 `queryOtaTasks(params)` 函数，对应全局分页查询接口；保留 getDeviceOtaTasks

#### Step 2.2 Dashboard 动态设备下拉（当前硬编码 sensor_1/2/3）
- **涉及文件**：
  - [Dashboard.vue](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/views/Dashboard.vue#L119-L124)
- **修改内容**：
  - 挂载时调用 `getDeviceList({ pageNum: 1, pageSize: 100 })` 拉取设备
  - deviceOptions 改为动态填充 `{ label: deviceName + '(' + deviceId + ')', value: deviceId }`
  - 默认选中第一个可用设备，若列表空则 fallback 到 sensor_1
  - 增加刷新按钮或轮询（可选，用户体验优化）

#### Step 2.3 DeviceList 校验与微调
- **涉及文件**：
  - [DeviceList.vue](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/views/devices/DeviceList.vue)
- **修改内容**：
  - 确认分页返回字段对齐（records / total）
  - handleEdit 当前仅允许编辑状态，考虑增加：可编辑 deviceName、model、location、description（可选增强）
  - 若后端统一 Result<T> 成功提示需正常显示（request.ts 已处理）

#### Step 2.4 FirmwareList 增加分页
- **涉及文件**：
  - [FirmwareList.vue](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/views/ota/FirmwareList.vue)
- **修改内容**：
  - 参考 DeviceList.vue 增加 pagination 响应式对象（page/size/total）
  - el-table 下方追加 el-pagination 组件
  - loadFirmwares 改为传 pageNum/size，返回列表从 records 取，total 赋值
  - handleSearch 重置 page=1

#### Step 2.5 OtaTaskList 增加全局列表视图（当前必须输入 deviceId 才能查）
- **涉及文件**：
  - [OtaTaskList.vue](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/views/ota/OtaTaskList.vue)
- **修改内容**：
  - 页面进入**默认展示全部任务分页列表**（不再显示 el-empty 提示输入）
  - 搜索栏改造：deviceId 变成可选筛选条件 + status 下拉（pending/downloading/installing/success/failed/cancelled）
  - 查询按钮调用新 queryOtaTasks 接口
  - 表格下方追加分页组件
  - 创建任务功能保留不变
  - 取消任务功能保留不变

#### Step 2.6 EnergyDashboard 字段校验（非破坏性，仅确认）
- **涉及文件**：
  - [EnergyDashboard.vue](file:///e:/AI/java/boonya-io/boonya-io-frontend/admin/src/views/energy/EnergyDashboard.vue)
- **修改内容**：
  - 后端 Step 1.3 改完后，逐项确认 5 个接口返回字段与前端使用一致
  - 重点：metricCards 4 张卡片字段、trend chart 的 4 条 series、areaRanking 表格、devices 表格、alarm 列表
  - 如有字段名不一致，改前端以对齐后端 DTO

---

## 四、潜在依赖与注意事项

### 4.1 响应格式过渡策略
- **风险**：DeviceController 从 ResponseEntity → Result<T> 后，前端 request.ts 判断逻辑是看 `res.code` 字段
- **策略**：后端改完后立即验证一次完整 CRUD，如前端解析异常，临时在 request.ts 中增加对 device 接口特殊处理（或先不统一 Result，等其他调完再最后一步统一）

### 4.2 MyBatis-Plus Page 返回字段
- 后端 `com.baomidou.mybatisplus.extension.plugins.pagination.Page` 字段：records / total / current / size / pages
- 前端 PageResult 接口已有对应字段，应兼容

### 4.3 OTA 任务量大时的性能
- 新增全局查询接口务必**按 createTime 建立索引**（entity 层如有 @TableIndex 注解加上）
- pageSize 上限设 100 防止一次拉爆

### 4.4 Energy DTO 字段对齐
- 前端 energy.ts 已定义完整接口类型，后端 DTO 必须与这些字段 1:1 对应，否则前端渲染空
- 特别注意枚举值：status=online/offline/warning，level=high/medium/low

### 4.5 构建/部署顺序
1. 先改完后端 4 个模块 → `mvn clean package -DskipTests`
2. `docker compose up -d --build`（4 个模块镜像重建，gateway 不用动）
3. 再改前端 → `npm run build`（本地开发或 Nginx 静态部署）

---

## 五、风险处理

| 风险项 | 影响 | 规避方案 |
|--------|------|----------|
| 统一 Result<T> 后前端解析异常 | 登录后所有 device 接口报错 | 分步：先完成其他改造，最后统一返回格式；改造过程中后端保留 ResponseEntity 临时过渡，通过开关或条件判断（建议一次性改完+联调） |
| Energy DTO 字段与前端不一致 | 能碳页所有图表空白 | 改完后立即用浏览器 F12 看 Network 返回，逐个字段核对 |
| Firmware/Ota 分页上线后，旧版前端调老接口 | 列表为空（因为老接口返回 List，新接口包了一层 Page） | firmware list 接口**向后兼容**：不传 pageNum/pageSize 时默认分页第一页返回 Page 结构；前端也要同步升级，不留旧前端调用链 |
| OTA 全局查询缺索引导致慢 | PostgreSQL 慢查询 | OtaTask 表 create_time 字段加索引（schema.sql 或 migration） |
| Dashboard 动态拉设备时列表为空 | 趋势图无设备可选 | 空列表时 fallback 到硬编码 sensor_1，并提示"暂无设备，先去注册" |

---

## 六、验证检查清单（Checklist）

### 后端改造完成后
- [ ] Device 所有接口返回带 `code=200` 的 Result JSON
- [ ] PUT /devices/{id}?status= 按主键直接更新生效（DB值变化）
- [ ] Energy 5 个接口返回字段名与 DTO 定义一致
- [ ] GET /firmware?pageNum=1&pageSize=20 返回 Page 结构（含 records/total）
- [ ] GET /ota/tasks?pageNum=1&pageSize=20 不传 deviceId 时返回全部任务
- [ ] 原有按 deviceId 查询 OTA 任务的旧接口仍可用（未删）

### 前端改造完成后
- [ ] 登录 → Dashboard：4 张卡片有值，设备下拉来自后端（非sensor_1/2/3字样），趋势图可切换设备正常渲染
- [ ] 设备管理：列表、搜索、分页、注册、详情、编辑状态、删除 全部正常
- [ ] 固件管理：列表分页、按型号/状态筛选、上传、发布、归档、删除 全部正常
- [ ] OTA 任务：默认进入展示全局分页列表；筛选 deviceId/status 生效；创建任务/取消任务正常
- [ ] 数据分析：已有功能保持不变（回归）
- [ ] 能碳管理：4 张指标卡片 + 4 系列趋势图 + 区域表格 + 设备表格 + 告警列表 全部有数据渲染
- [ ] 登出正常，Token 过期自动刷新逻辑（request.ts）不报错

---

## 七、改造优先级（建议执行顺序）

1. **P0 必改**：Step 1.5 + 2.5（OTA 全局任务列表，当前完全没法看全部任务）
2. **P0 必改**：Step 2.2（Dashboard 设备下拉硬编码，不动态的话看板是假数据）
3. **P1 重要**：Step 1.4 + 2.4（固件分页，数据量大必崩）
4. **P1 重要**：Step 1.3 + 2.6（能碳强类型化，避免Map错位）
5. **P2 规范化**：Step 1.1 + 1.2（Device Result 统一 + 简化查询）
6. **P2 校验**：Step 2.1 + 2.3（API 层对齐 + DeviceList 微调）
