# Boonya IoT Admin 后台接口与页面调整计划

## 摘要

Admin 后台目前仅"数据分析页"基本完整，其余模块存在接口格式不统一、分页缺失、前端硬编码/mock 残留等问题。本计划以 **OTA 模块**（已完整实现 `Result<T>` + `PageResult<T>` 分页）为标杆，统一其余模块的响应格式与分页，并修复前端残留的硬编码/mock，使所有页面可用。

> 注：OTA 任务分页（后端 + 前端）与固件列表分页（后端 + 前端）已在本轮早些时候完成，本计划不再涉及，仅记录为"已完成"。

---

## 当前状态分析（基于实际代码探查）

### 前端（`boonya-io-frontend/admin/src`，共 7 页）

| 页面 | 路径 | 状态 | 问题 |
|---|---|---|---|
| Login | `views/Login.vue` | 完整 | — |
| DeviceList | `views/devices/DeviceList.vue` | 完整(CRUD) | 依赖后端裸 `Page`/`ResponseEntity`，靠 `request.ts` 兼容层勉强工作 |
| FirmwareList | `views/ota/FirmwareList.vue` | **已完成分页** | — |
| OtaTaskList | `views/ota/OtaTaskList.vue` | **已完成全局分页** | — |
| DataAnalytics | `views/analytics/DataAnalytics.vue` | 完整 | — |
| EnergyDashboard | `views/energy/EnergyDashboard.vue` | 部分可用 | `loadData()` 无 try/catch；后端返回松散 `Map`，前端类型为 `any` |
| **Dashboard** | `views/Dashboard.vue` | **半废** | 设备下拉 `deviceOptions` 硬编码 `sensor_1/2/3`（第 119-124 行），未调任何设备 API |

### 后端（8 个含 Controller 的模块）

| 模块 | Result 包装率 | 分页 | 主要问题 |
|---|---|---|---|
| auth | 100% | — | register 用 @RequestParam（小问题） |
| **device** | **0%** | 裸 MP `Page` | 全裸 `ResponseEntity`，`PUT /{id}` 仅改 status 且二次查库 |
| iot-Device | 0% | 无 | 无分页，裸 List |
| iot-Rule | 100% | — | 404 手写 |
| analytics-Dashboard | 100%外壳 | — | trend/overview 用松散 Map |
| **analytics-Energy** | 100%外壳 | 无 | **全松散 Map + service 硬编码 mock 数据** |
| ota | 100% | 标准 PageResult | 标杆，已完成 |
| minio-File | 0% | 无 | 裸 ResponseEntity |

公共类：`Result<T>`（`com.boonya.lab.io.common.response.Result`，code/message/data/timestamp）、`PageResult<T>`（同包，current/size/total/pages/records，`PageResult.of(current,size,total,records)`）。前端 `api/device.ts` 已定义对应的 `PageResult<T>` 接口（records/total/current/size/pages）。

---

## 已完成（本轮早些时候，不再改动）

- 后端 OTA：`OtaTaskRepository`/`FirmwareRepository` 加 `JpaSpecificationExecutor`；`OtaTaskService.queryTasks`、`FirmwareService` 分页；`OtaTaskController`/`FirmwareController` 返回 `Result<PageResult<...>>`。
- 前端：`api/ota.ts` 增 `queryOtaTasks`；`api/firmware.ts` 改 `getFirmwareList` 返回 `PageResult`；`OtaTaskList.vue` 改全局分页视图；`FirmwareList.vue` 加分页组件。

---

## 待办变更

### Step 1（P0，纯前端）：Dashboard 动态设备下拉

**文件**：`boonya-io-frontend/admin/src/views/Dashboard.vue`

**问题**：第 119-124 行 `deviceOptions` 硬编码 3 个 sensor，用户无法选真实设备；`api/device.ts` 已有 `getOnlineDevices()`（`GET /devices/online`）却未被引用。

**改法**：
1. `import { getOnlineDevices } from '@/api/device'` 及 `import type { Device } from '@/api/device'`。
2. 删除硬编码数组，改为 `const deviceOptions = ref<{label:string;value:string}[]>([])`。
3. 新增 `loadDeviceOptions()`：调 `getOnlineDevices()`，映射为 `{ label: deviceName||deviceId, value: deviceId }`；空列表时保持空数组（下拉显示 placeholder）。
4. `onMounted` 中先 `await loadDeviceOptions()`，若列表非空则 `selectedDeviceId.value = deviceOptions.value[0].value`，再 `loadTrend()`；为空时跳过 `loadTrend`（图表保持空态）。
5. `loadTrend` 已有 `if(!chart) return` 守卫，无需改动。

**为什么**：消除最后一处前端 mock 残留，让趋势图可切真实在线设备。

---

### Step 2（P1，后端）：Device 模块统一 `Result<T>` + 简化更新逻辑

**文件**：`boonya-io-device/src/main/java/com/boonya/lab/io/device/controller/DeviceController.java`

**问题**：10 个端点全部裸 `ResponseEntity`；`/query` 返回 MyBatis-Plus 原生 `Page` 而非 `PageResult`；`PUT /{id}` 先 `getDevice(id)` 再 `updateDeviceStatus(deviceId,status)` 二次查库。

**改法**：
1. 引入 `com.boonya.lab.io.common.response.{Result,PageResult}`。
2. 各端点返回类型由 `ResponseEntity<X>` 改为 `Result<X>`，body 用 `Result.success(...)`：
   - `registerDevice` → `Result<DeviceResponse>`
   - `activateDevice`/`updateHeartbeat`/`deleteDevice` → `Result<Void>`（`Result.success()`）
   - `getDevice`/`getDeviceByDeviceId` → `Result<DeviceResponse>`
   - `queryDevices` → `Result<PageResult<DeviceResponse>>`：`Page<DeviceResponse> p = service.queryDevices(req); return Result.success(PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(), p.getRecords()));`
   - `getOnlineDevices` → `Result<List<DeviceResponse>>`
   - `getDeviceStatus` → `Result<Map<String,String>>`（保留 Map 或新建小 DTO，此处保留 Map 以缩小改动）
   - `updateDeviceStatus` → `Result<Void>`
3. **简化更新逻辑**：在 `DeviceService` 增 `updateDeviceStatusById(Long id, String status)` 直接按 id 更新（避免先查后改）；controller 直接调它。若 service 改动较大则保留现状仅统一格式——优先保证格式统一，简化为可选。
4. `@Tag`/`@Operation` 注解保留。

**前端影响**：`request.ts` 已兼容 `Result` 包装与裸数据，`DeviceList.vue` 无需改动即可继续工作（响应外壳从裸数据变 `Result.data`，兼容层会处理）。`api/device.ts` 的 `PageResult` 字段（records/total/current/size）与后端 `PageResult` 一致，无需改类型。

**为什么**：消除全站最大的格式割裂，使 device 与 ota/analytics 一致；分页字段标准化便于后续维护。

---

### Step 3（P1，后端+前端）：能碳分析强类型 DTO 化 + 前端字段校验

#### 3a 后端 DTO 化

**新增文件**（`boonya-io-analytics/src/main/java/com/boonya/lab/io/analytics/dto/`）：
- `EnergyOverviewDTO`：siteName, period(String), electricityKwh, waterM3, solarKwh, storageDischargeKwh, energyCostCny, carbonTons, carbonReductionTons(Double), activeAlarms(Integer), onlineDevices(Integer), totalDevices(Integer)
- `EnergyTrendItemDTO`：time, electricityKwh, waterM3, solarKwh, carbonTons(Double)
- `AreaRankingDTO`：name, electricityKwh, waterM3, carbonTons(Double), trend(String)
- `EnergyDeviceStatusDTO`：deviceId, deviceName, deviceType, status, value(Double), unit, location
- `EnergyAlarmDTO`：level, title, deviceId, description, status, time(String)

**修改 `EnergyAnalyticsService.java`**：5 个方法返回类型由 `Map<String,Object>`/`List<Map>` 改为对应 DTO/`List<DTO>`；内部 mock 数值保留不变（仅结构化）。私有 `area()/device()/alarm()` 工厂方法改为构造 DTO。

**修改 `EnergyAnalyticsController.java`**：返回类型由 `Result<Map<String,Object>>` 等改为 `Result<EnergyOverviewDTO>` 等。

> 决策：本轮**仅 DTO 化**，保留现有 mock 数值；接入真实能源数据源（电表/水表/光伏逆变器）超出本轮范围，列为后续工作。

#### 3b 前端类型化 + 字段校验

**修改 `boonya-io-frontend/admin/src/api/energy.ts`**：用上述 DTO 字段定义 TS interface 替换 `any`，各函数返回类型对齐。

**修改 `boonya-io-frontend/admin/src/views/energy/EnergyDashboard.vue`**：
- `loadData()` 包 `try/catch`：任一接口失败时降级（保留上次数据或置默认），`ElMessage.error` 提示，避免整页 reject。
- 各取值加防御默认（如 `overview.electricityKwh ?? 0`）。

**为什么**：后端有契约、前端有类型，避免字段漂移；前端容错避免单接口失败白屏。

---

## 假设与决策

1. **能碳数据**：本轮仅强类型 DTO 化，保留 mock 数值，不接真实数据源（无现成数据来源）。
2. **设备更新**：保持"仅状态更新"，不新增设备元数据编辑接口（前端 DeviceList 当前也仅编辑状态，已对齐）。
3. **OTA/Firmware**：已完成，不再改动。
4. **暂不处理**（列为后续）：minio `FileController` 裸 ResponseEntity；`GlobalExceptionHandler` HTTP 状态码恒 200、校验错误未回传 data；iot 模块 Device 历史无分页。这些不影响 admin 页面可用性。
5. 前端 `request.ts` 兼容层可同时处理 `Result` 包装与裸数据，故 Step 2 后端格式切换不会破坏现有前端。

---

## 验证步骤

**后端**：
- 各模块 `mvn -q -pl <module> compile` 通过编译。
- 启动后用 curl/Postman 验证：
  - `GET /api/devices/query?pageNum=1&pageSize=10` 返回 `Result<PageResult<DeviceResponse>>` 结构。
  - `GET /api/analytics/energy/overview` 返回 `Result<EnergyOverviewDTO>`，字段名与 DTO 一致。
  - `GET /api/devices/online` 返回 `Result<List<DeviceResponse>>`。

**前端**：
- `cd boonya-io-frontend/admin && npm run build` 通过类型检查。
- 浏览器逐页点击：
  - Dashboard 设备下拉显示真实在线设备，切换后趋势图刷新。
  - EnergyDashboard 接口失败时不再白屏，有错误提示。
  - DeviceList 列表/分页/状态编辑正常。
  - OTA/Firmware 页面分页正常（回归）。

---

## 实施顺序

1. Step 1 Dashboard 动态下拉（纯前端，最快见效）
2. Step 2 Device 后端统一 Result（后端，影响面广需回归 DeviceList）
3. Step 3 能碳 DTO 化 + 前端校验（后端+前端）
