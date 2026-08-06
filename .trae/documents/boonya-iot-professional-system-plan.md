# Boonya IoT 专业物联网系统总体规划

> 以物联网专家视角，将 README.md 技术规划中的全部核心元素端到端贯通，打造专业级 IoT 平台。
> 本计划基于 2026-08-06 实际源码核实（README v1.1 多处已过时，已校正）。

---

## 一、真实现状（核实后，非 README 旧描述）

### 后端
| 模块 | 实际状态 |
|---|---|
| gateway | ✅ JWT 过滤器 + 5 路由 |
| auth | ✅ 登录/刷新/登出/注册；RBAC 三表已建（sys_user/sys_role/sys_user_role） |
| device | ⚠️ 业务可用，但 0% `Result` 包装、裸 MP `Page`、仅状态更新 |
| iot | ✅ **核心链路全通**：MQTT 订阅→TDengine 写入→规则引擎(0 TODO)→告警→WebSocket STOMP(`/topic/alerts`、`/topic/device/{id}`、`/topic/stored`)；AI 分析=真实时序数据+统计算法(线性回归/标准差)，非 LLM 非 mock |
| minio | ⚠️ 文件操作可用，裸 `ResponseEntity` |
| analytics | ✅ DashboardService 接 TDengine(JdbcTemplate)；🔴 EnergyAnalyticsService **纯硬编码 mock，零数据源** |
| ota | ✅ 完整且标准化（Result+PageResult） |
| cache | 工具库 |

### 前端
- **Admin（7 页）**：6 页功能完整（OTA/固件/设备/数据分析/能源部分可用），Dashboard 动态下拉本轮已修复。🔴 **零 WebSocket 接入**——实时能力用户不可见。
- **H5（仅 3 页）**：登录/设备列表(真实分页)/设备详情(HTTP 快照)。路由守卫✅、Token 刷新✅。🔴 缺固件/OTA/告警/分析/仪表盘；🔴 零 WebSocket。

### 核心断层（"未融合"的 IoT 元素）
1. 🔴 **实时监控链路前端断点**：后端 WS 已全通，Admin/H5 均未订阅 —— IoT 最核心能力对用户不可见
2. 🔴 **能碳纯 Mock**：有独立商业化文档(Boonya EMS)要做成产品，实际零数据源
3. 🟡 **TDengine 单值 schema**：当前超表只有 `ts_value`，能碳多指标(电/水/光)需扩展
4. 🟡 **后端格式割裂**：device/minio 裸 ResponseEntity、energy 松散 Map
5. 🟡 **RBAC 无管理界面**、**AI 无前端页**
6. 🟡 **H5 不完整**

### 已完成（本轮）
- OTA/固件 后端分页 + 前端分页视图
- Dashboard 设备下拉动态加载（`getOnlineDevices`）

---

## 二、总体规划（分阶段，P0→P1→后续）

### Phase 1（P0）：贯通实时监控核心链路 —— IoT 标志性能力

**目标**：让后端已就绪的 WebSocket 推送在前端"看得见"，闭环 MQTT→TDengine→规则→告警→前端实时展示。

#### 1.1 网关 WebSocket 路由确认
- **文件**：`boonya-io-gateway/src/main/resources/application-docker.yml`、`application-localhost.yml`
- **动作**：确认 iot 服务路由对 `/ws` 及 `ws://` 升级转发正常；若 Gateway 未显式处理 WS，需确保 route 的 `Predicates` 含 `Path=/ws/**` 且未阻断 Upgrade 头。Spring Cloud Gateway 原生支持 WS 路由，通常无需额外配置，仅需验证。
- **验证**：浏览器 `new SockJS('http://localhost:8080/ws')` 能握手成功。

#### 1.2 Admin 实时接入
- **新增** `boonya-io-frontend/admin/src/composables/useWebSocket.ts`：封装 SockJS+STOMP（`@stomp/stompjs` + `sockjs-client`，需 `npm i`）。提供 `connect()`、`subscribe(topic, cb)`、`disconnect()`，复用 localStorage token 鉴权（若 WS 需鉴权则连前注入）。
- **新增** `api/realtime.ts`（或并入 analytics）：定义告警/遥测 TS 类型。
- **改 `layouts/MainLayout.vue`**：顶栏 `header-right` 加告警铃铛 `<el-badge>`，`onMounted` 连 WS 订阅 `/topic/alerts`，新告警 `ElNotification` 弹窗 + 累计计数；点击跳告警列表。
- **改 `views/Dashboard.vue`**：选定设备后订阅 `/topic/device/{deviceId}`，实时刷新温度趋势图（追加最新点）。
- **改 `views/analytics/DataAnalytics.vue`**：实时卡片订阅 `/topic/device/{deviceId}` 刷新最新值。
- **新增 `views/alerts/AlertList.vue` + 路由**：告警历史（先用 WS 内存累计 + 后续可补后端告警持久化接口）。

#### 1.3 H5 实时接入
- **改 `h5/src/views/DeviceDetail.vue`**：`toggleRealtime` 改为订阅 `/topic/device/{deviceId}`（替换 HTTP 快照），`onUnmounted` 取消订阅。
- **新增 `h5/src/composables/useWebSocket.ts`**：与 Admin 对称封装。
- **新增 `h5/src/views/AlertList.vue` + 路由**：订阅 `/topic/alerts`，下拉刷新。

**为什么**：这是把"后端能干"变成"用户能用"的关键一跃，是专业 IoT 平台区别于普通 CRUD 后台的标志性能力。

---

### Phase 2（P0）：能碳真实数据源 + 强类型 DTO（兑现 Boonya EMS 商业化）

**目标**：将纯 mock 的能碳模块接入真实时序数据，兑现商业化文档"看得见/管得住/算得清"。

#### 2.1 TDengine 多指标 schema 扩展
- **约束**：当前超表 `iot.devices` 仅 `ts_value` 单值，无法承载电/水/光多指标。
- **方案**（务实）：新增能碳专用超级表 `iot.energy_metrics`（标签：`device_id`、`metric_type`[electricity/water/solar/storage]；字段：`ts`、`value`、`unit`）。能碳设备（电表/水表/逆变器）经 MQTT 上报到 `device/{id}/energy` 主题，由 iot 服务写入此超表。
- **文件**：
  - `boonya-io-iot/src/main/java/.../service/MqttSubscriber.java`：新增 `device/+/energy` 订阅与解析，写入 `iot.energy_metrics`。
  - `boonya-io-iot/src/main/java/.../service/TimeSeriesService.java`：新增 `saveEnergyMetric(deviceId, metricType, value, unit)`。
- **设备模拟器**：`boonya-io-iot/.../device/DeviceSimulator.java` 增加能碳设备模拟（电表/水表/光伏定期上报）。

#### 2.2 能碳设备档案
- **新增** `boonya-io-analytics` 或 `boonya-io-device`：`energy_device` 表（device_id、device_type[meter/water_meter/solar_inverter/storage_bms]、area、ratio、location、status）+ JPA 实体 + Repository。schema.sql 增建表语句。
- 用于 `getDeviceStatus()`、`getAreaRanking()` 的设备维度来源。

#### 2.3 EnergyAnalyticsService 接真实数据
- **重写 `EnergyAnalyticsService.java`**：注入 `JdbcTemplate`（同 DashboardService 范式），查询 `iot.energy_metrics` 聚合：
  - `getOverview()`：`SUM(value) WHERE metric_type=? AND ts>=今日` 得电/水/光总量；碳排=电量×排放因子(0.5703 kgCO2/kWh)。
  - `getEnergyTrend(period)`：`INTERVAL()` 或按时段 `GROUP BY` 聚合。
  - `getAreaRanking()`：JOIN `energy_device` 按 area 聚合。
  - `getDeviceStatus()`：查 `energy_device` 档案 + 各设备最新值。
  - `getAlarms()`：阈值检测（夜间异常、功率越限）——可复用规则引擎或独立阈值查询。
- **DTO 化**：新增 5 个 DTO（`EnergyOverviewDTO`/`EnergyTrendItemDTO`/`AreaRankingDTO`/`EnergyDeviceStatusDTO`/`EnergyAlarmDTO`），service 返回 DTO；controller 返回 `Result<DTO>`。

#### 2.4 前端类型化 + 容错
- **`api/energy.ts`**：用 DTO 字段定义 TS interface 替换 `any`。
- **`views/energy/EnergyDashboard.vue`**：`loadData()` 包 try/catch（任一接口失败降级+`ElMessage.error`），取值加 `?? 0` 防御。

**为什么**：能碳是 README 技术栈与商业化文档的双重重点，纯 mock 是最大"废"点；接入真实数据后才是可交付的 EMS。

---

### Phase 3（P1）：后端格式统一 + RBAC + AI 页面

#### 3.1 Device 统一 `Result<T>`
- **文件**：`boonya-io-device/.../controller/DeviceController.java`
- 10 端点 `ResponseEntity<X>` → `Result<X>`；`/query` 的 `Page<DeviceResponse>` → `PageResult.of(...)`。保留状态-only 更新（与前端对齐）。`request.ts` 兼容层保证前端不破。

#### 3.2 MinIO 统一 `Result<T>`
- **文件**：`boonya-io-minio/.../controller/FileController.java`
- 4 端点裸 `ResponseEntity` → `Result<T>`（上传返回 `Result<Map<String,String>>` 含 url/objectName；下载/预签名保留特殊处理或包 `Result`）。

#### 3.3 RBAC 用户/角色管理
- **后端** `boonya-io-auth`：新增 `UserController`（用户分页查询/创建/禁用/重置密码/分配角色）+ `RoleController`（角色 CRUD）。复用已有 sys_user/sys_role/sys_user_role 表。返回 `Result<T>`+`PageResult`。
- **前端**：新增 `api/user.ts`、`views/system/UserList.vue` + 路由（菜单自动出现）。BCrypt 加密复用。

#### 3.4 AI 分析页（算法型，可选接 LLM）
- **后端** `boonya-io-iot/.../AiAnalysisController`：将 `Map` 返回改为强类型 DTO（`DeviceDiagnosisDTO`/`TrendPredictionDTO`），保留现有统计算法（真实数据驱动，已是专业实现）。
- **前端**：新增 `api/ai.ts`、`views/ai/AiAnalysis.vue` + 路由：选设备→诊断报告（异常检测）+趋势预测（线性回归曲线）。
- **LLM 接入**列为后续：父 pom 已有 spring-ai 依赖，后续注入 `ChatClient` 替换算法层，契约不变。

**为什么**：补齐"权限管理"与"智能分析"两个 README 列出但前端缺失的专业能力。

---

### Phase 4（P1）：H5 补全 + 类型安全 + 收尾

#### 4.1 H5 页面补全
- 新增 `h5/src/views/`：`Dashboard.vue`（移动看板）、`AlertList.vue`（Phase 1 已含）、`DataAnalytics.vue`（简化分析）。
- 路由补全，菜单/底部 Tab 栏调整。
- 固件/OTA 在 H5 可暂缓（移动端低频），优先实时+告警+看板。

#### 4.2 类型安全与 API 层统一
- 全局清理 `any`（`userInfo`、`firmwares`、`deviceInfo` 等），对齐已定义 interface。
- H5 `api/` 目录补齐（固件/OTA/告警/分析），与 Admin 类型共享（可抽 `shared/types` 或各自定义）。

#### 4.3 验证
- 后端：各模块 `mvn -q compile`；curl 验证 `/api/devices/query` 返回 `Result<PageResult>`、`/api/analytics/energy/overview` 返回真实聚合数据。
- 前端：`npm run build` 类型检查通过；浏览器端到端：设备上报→前端实时刷新→告警弹窗→能碳看板真实数据。

---

### Phase 5（后续，列出但本轮不做）
Nacos 注册配置中心、Kafka 削峰、Seata 分布式事务、SkyWalking/Zipkin 链路追踪、Prometheus+Grafana 指标、ELK 日志中心、K8s 部署、LLM 智能分析、多租户 SaaS。这些属架构演进，待业务功能闭环后迭代。

---

## 三、假设与决策

1. **实时链路**为本轮 P0 主干（专业 IoT 标志性能力），后端已就绪，重点是前端接入。
2. **能碳**本轮接真实数据源（扩展 TDengine 多指标超表 + 能碳设备档案），兑现 EMS 商业化，非仅 DTO 化。
3. **AI** 保留现有真实数据驱动的统计算法（已专业），本轮加 DTO+前端页；LLM 接入列后续。
4. **RBAC** 复用已建三表，补管理接口+页面。
5. **device/minio** 统一 Result 格式，device 保持状态-only 更新。
6. **H5** 本轮补实时+告警+看板（移动端高频场景），固件/OTA 暂缓。
7. 架构演进 P2 项本轮不做，列 Phase 5。
8. `request.ts` 兼容层使后端格式切换不破坏前端。

---

## 四、实施顺序与依赖
1. Phase 1 实时链路（前端为主，最快体现"专业 IoT"）
2. Phase 2 能碳真实化（后端为主，schema 扩展是前置）
3. Phase 3 格式统一 + RBAC + AI 页（并行可推进）
4. Phase 4 H5 补全 + 收尾验证

每个 Phase 完成后做编译+构建+端到端验证再进入下一 Phase。
