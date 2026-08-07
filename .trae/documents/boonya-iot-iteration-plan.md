# Boonya IoT 迭代计划 v2

> 范围：修复关键 Bug + 告警闭环体系 + RBAC 权限落地 + 前端补齐页面
> 不含：Nacos/Kafka/SkyWalking/Prometheus 架构演进（留待下一轮）

---

## 一、当前状态分析

### 已验证可运行
- 20 个 Docker 容器全部启动，核心基础设施 healthy
- TDengine 遥测数据 39 万+条，能碳数据持续上报
- 能碳总览/趋势 API 返回真实数据
- 前端 10 个页面均有真实业务逻辑，无空壳页

### 发现的关键 Bug（4 个）
1. **`TimeSeriesService.flushPendingWrites()` 数据丢失**（第 170-189 行）：TDengine 断线恢复后从队列取出待写数据但只打印日志，未调用 `doSave()` 实际写入，导致断网期间采集数据永久丢失
2. **`AuthService.login()` 角色硬编码**（第 69 行）：返回 `Collections.singletonList("admin")`，未从 DB 查询用户实际角色，token 中角色信息与真实角色不符
3. **网关路由冲突**：iot 模块 `DeviceController` 路径 `/api/devices` 与 device 模块冲突，gateway 将 `/api/devices/**` 路由到 device，导致 iot 的 `/api/devices/{id}/history` 和 `/logs` 网关不可达；`/api/rules/**` 未在 gateway 配置路由
4. **MinIO `@PathVariable` 路径参数 Bug**：`FileController` 的 `deleteFile`/`checkFileExists`/`getTemporaryAccessUrl` 用 `@PathVariable String objectName`，但 MinIO 对象名常含 `/`（如 OTA 固件路径），路径参数无法捕获含 `/` 的路径段

### 功能缺口
- **告警体系碎片化**：iot 模块 AlertHandler 仅 WebSocket/MQTT 推送不持久化；analytics 模块 getAlarms() 实时计算不存储；无统一告警表、无状态流转、无历史查询
- **RBAC 仅有壳无核**：auth 存了 role_code 但全项目无 @PreAuthorize；gateway 只认证不授权；前端路由守卫仅检查 token 存在
- **规则未持久化**：RuleEngine 用 ConcurrentHashMap，重启即丢失
- **DeviceLogService 是 MOCK**：内存存储，重启丢失，未关联 device_log 表
- **前端缺失页面**：告警规则管理页、角色管理页、设备历史/日志页；设备激活按钮、用户编辑功能未实现

---

## 二、实施计划

### Phase 1：修复关键 Bug（4 项）

#### 1.1 修复 TimeSeriesService 数据丢失 Bug
- **文件**：`boonya-io-iot/src/main/java/com/boonya/lab/io/iot/service/TimeSeriesService.java`
- **问题**：`flushPendingWrites()` 方法第 170-189 行从 `pendingWrites` 队列取出数据后仅 `log.info`，未调用 `doSave()` 实际写入 TDengine
- **修复**：在 `poll()` 后调用 `doSave(record.deviceId(), record.ts(), record.temp())` 实际写入；添加写入结果计数日志；限制单次 flush 数量防止阻塞
- **验证**：模拟 TDengine 断线→恢复→检查 pendingWrites 队列清空且数据写入

#### 1.2 修复 AuthService 登录角色硬编码
- **文件**：`boonya-io-auth/src/main/java/com/boonya/lab/io/auth/service/AuthService.java`
- **问题**：第 69 行 `roles = Collections.singletonList("admin")`，未从 DB 查询用户实际角色
- **修复**：通过 `UserRoleMapper` 查询用户关联的角色 code 列表；若用户无角色则返回空列表（非 admin）；在 `LoginResponse` 中返回真实角色
- **验证**：用非 admin 角色用户登录，检查 token 中 roles 与 DB 一致

#### 1.3 修复网关路由冲突
- **文件**：`boonya-io-gateway/src/main/resources/application-docker.yml` + `boonya-iot/src/main/java/.../controller/DeviceController.java`
- **问题**：
  - iot 模块的 `DeviceController` 路径 `/api/devices` 与 device 模块冲突
  - `/api/rules/**` 未在 gateway 配置路由
- **修复**：
  - 将 iot 模块的 `DeviceController` 路径从 `/api/devices` 改为 `/api/iot/devices`，消除冲突
  - 在 gateway `application-docker.yml` 添加 `/api/rules/**` 路由到 iot-app 服务
  - 更新前端 `ai.ts` 中 AI 分析接口路径（如果引用了旧路径）
- **验证**：通过网关访问 `/api/rules`、`/api/iot/devices/{id}/history` 返回 200

#### 1.4 修复 MinIO 路径参数 Bug
- **文件**：`boonya-io-minio/src/main/java/com/boonya/lab/io/minio/controller/FileController.java`
- **问题**：`@PathVariable String objectName` 无法捕获含 `/` 的路径段
- **修复**：将 `@PathVariable` 改为 `@RequestParam` 或使用 `@PathVariable("**") String objectName` 捕获多级路径；同步更新 `MinioService` 方法签名
- **验证**：对含 `/` 的对象名调用 delete/exists/url 接口返回正常

---

### Phase 2：告警闭环体系

#### 2.1 后端：统一告警持久化（device 模块）
- **新建文件**：
  - `boonya-io-device/src/main/java/.../entity/Alert.java` — 告警实体（id, deviceId, alertType, severity, title, message, metricValue, threshold, status, triggerTime, ackTime, resolveTime, operator）
  - `boonya-io-device/src/main/java/.../repository/AlertRepository.java` — JPA Repository + Specification 动态查询
  - `boonya-io-device/src/main/java/.../service/AlertManageService.java` — 告警管理服务（分页查询、状态流转、统计）
  - `boonya-io-device/src/main/java/.../controller/AlertController.java` — 告警管理 API
- **修改文件**：
  - `boonya-io-device/src/main/resources/schema.sql` — 添加 `device_alert` 表
- **API 设计**：
  ```
  GET    /api/alerts?pageNum=1&pageSize=20&deviceId=&severity=&status=&startTime=&endTime=
  GET    /api/alerts/{id}
  PUT    /api/alerts/{id}/acknowledge    — 确认告警
  PUT    /api/alerts/{id}/resolve        — 解决告警
  PUT    /api/alerts/{id}/close          — 关闭告警
  GET    /api/alerts/statistics          — 告警统计（按级别/状态/设备聚合）
  ```
- **告警状态流转**：`PENDING → ACKNOWLEDGED → RESOLVED → CLOSED`

#### 2.2 后端：iot 模块告警写入
- **修改文件**：`boonya-io-iot/src/main/java/.../event/handler/AlertHandler.java`
- **修改内容**：AlertHandler 触发告警时，除了 WebSocket/MQTT 推送，同时通过 HTTP 调用 device 模块的内部 API 持久化告警记录
  - 使用 `RestTemplate` 或 `WebClient` 调用 `POST http://iot-device:8082/api/alerts/internal`（内部接口，不走网关）
  - 或改用更简洁方案：直接在 iot 模块用 JdbcTemplate 写入 device 模块的 `device_alert` 表（共享 PostgreSQL `iot_device` 库）
- **决策**：采用 iot 模块直连 `iot_device` 库写入 `device_alert` 表方案，避免服务间 HTTP 调用复杂度

#### 2.3 后端：规则引擎持久化
- **修改文件**：`boonya-io-iot/src/main/java/.../service/RuleEngine.java`
- **修改内容**：
  - 新建 `AlertRule` 实体 + `AlertRuleRepository`（JPA，连接 `iot_device` 库或新建 `iot_rule` 库）
  - `RuleEngine` 启动时从 DB 加载规则到内存 Map；CRUD 操作同步写 DB
  - `initDefaultRules()` 改为仅在表为空时插入默认规则
- **新建文件**：
  - `boonya-io-iot/src/main/java/.../entity/AlertRule.java`
  - `boonya-io-iot/src/main/java/.../repository/AlertRuleRepository.java`
- **schema.sql** 添加 `alert_rule` 表

#### 2.4 前端：告警管理页面增强
- **修改文件**：`boonya-io-frontend/admin/src/views/alerts/AlertList.vue`
- **修改内容**：
  - 从仅展示 MQTT 实时告警 → 改为 Tab 双模式：「实时告警」(现有) + 「历史告警」(新增)
  - 历史告警 Tab：分页表格 + 筛选（设备/级别/状态/时间范围）+ 状态流转操作（确认/解决/关闭按钮）
  - 告警统计卡片（今日总数/待处理/已确认/已解决）
- **新增 API**：`boonya-io-frontend/admin/src/api/alert.ts`
  ```ts
  getAlerts(params) / getAlertById(id) / acknowledgeAlert(id) / resolveAlert(id) / closeAlert(id) / getAlertStatistics()
  ```

---

### Phase 3：RBAC 权限落地

#### 3.1 后端：权限数据模型
- **修改文件**：`boonya-io-auth/src/main/resources/schema.sql`
- **新增表**：
  ```sql
  sys_permission (id, parent_id, name, code, type[MENU/BUTTON/API], path, component, icon, sort, enabled)
  sys_role_permission (id, role_id, permission_id)
  ```
- **新建文件**：
  - `boonya-io-auth/src/main/java/.../entity/Permission.java`
  - `boonya-io-auth/src/main/java/.../entity/RolePermission.java`
  - `boonya-io-auth/src/main/java/.../repository/PermissionRepository.java`
  - `boonya-io-auth/src/main/java/.../repository/RolePermissionRepository.java`
- **初始数据**：插入菜单权限树（Dashboard/设备管理/固件管理/OTA任务/数据分析/告警管理/能碳看板/系统管理/AI分析）

#### 3.2 后端：登录返回权限树
- **修改文件**：`boonya-io-auth/src/main/java/.../service/AuthService.java`
- **修改内容**：登录时查询用户角色 → 通过 `sys_role_permission` 关联查询 `sys_permission` → 返回权限 code 列表 + 菜单树
- **修改文件**：`boonya-io-auth/src/main/java/.../dto/LoginResponse.java` — 添加 `permissions: List<String>` 和 `menus: List<MenuNode>` 字段
- **修改文件**：`boonya-io-auth/src/main/java/.../service/UserManageService.java` — 用户详情返回角色关联的权限

#### 3.3 后端：权限管理 API
- **新建文件**：`boonya-io-auth/src/main/java/.../controller/PermissionController.java`
- **API 设计**：
  ```
  GET    /api/permissions/tree              — 权限树
  GET    /api/permissions                   — 权限列表
  POST   /api/permissions                   — 创建权限
  PUT    /api/permissions/{id}              — 更新权限
  DELETE /api/permissions/{id}              — 删除权限
  GET    /api/roles/{id}/permissions        — 角色已分配权限
  PUT    /api/roles/{id}/permissions        — 分配角色权限（覆盖式）
  ```

#### 3.4 后端：网关授权过滤
- **修改文件**：`boonya-io-gateway/src/main/java/.../filter/AuthenticationFilter.java`
- **修改内容**：
  - 解析 JWT 中的 `permissions` claim
  - 请求路径与方法映射到所需权限 code（如 `GET:/api/devices` → `device:list`）
  - 无所需权限返回 403
  - 白名单路径（登录/注册/swagger/actuator）跳过授权检查
- **决策**：权限映射采用「路径前缀 + HTTP 方法 → 权限 code」配置表，存在 Redis 或 application.yml 中，避免硬编码

#### 3.5 前端：动态路由 + 菜单权限
- **修改文件**：`boonya-io-frontend/admin/src/router/index.ts`
- **修改内容**：
  - 将静态路由拆分为 `constantRoutes`（login/404）+ `asyncRoutes`（业务页面）
  - 登录后根据返回的 `menus` 动态注册路由（`router.addRoute`）
  - 路由守卫：无权限路由不注册，访问时跳 403
- **修改文件**：`boonya-io-frontend/admin/src/stores/user.ts`
- **修改内容**：存储 `permissions` 和 `menus`；添加 `hasPermission(code)` 方法
- **修改文件**：`boonya-io-frontend/admin/src/layouts/MainLayout.vue`
- **修改内容**：侧边栏菜单根据 `menus` 动态渲染，非权限菜单不显示
- **新建文件**：`boonya-io-frontend/admin/src/directives/permission.ts` — `v-permission` 指令，控制按钮级显隐
- **修改文件**：`boonya-io-frontend/admin/src/api/user.ts` — 添加权限管理 API

---

### Phase 4：前端补齐页面

#### 4.1 告警规则管理页面
- **新建文件**：`boonya-io-frontend/admin/src/views/alerts/AlertRuleList.vue`
- **功能**：规则分页列表 + 筛选（设备/状态）+ 新增/编辑规则弹窗（指标类型/运算符/阈值/冷却时间/启用状态）+ 启用/禁用开关 + 删除
- **新增 API**：在 `alert.ts` 中添加 `getAlertRules/createAlertRule/updateAlertRule/deleteAlertRule/toggleAlertRule`
- **路由**：`/alert-rules` → `AlertRuleList`

#### 4.2 角色管理页面
- **新建文件**：`boonya-io-frontend/admin/src/views/system/RoleList.vue`
- **功能**：角色分页列表 + 新增/编辑角色弹窗 + 分配权限弹窗（权限树 checkbox）+ 删除
- **修改 API**：`user.ts` 已有角色 CRUD API，补充 `getRolePermissions/assignRolePermissions`
- **路由**：`/system/roles` → `RoleList`

#### 4.3 设备历史数据/日志页面
- **新建文件**：`boonya-io-frontend/admin/src/views/devices/DeviceDetail.vue`
- **功能**：
  - Tab 1：设备信息（元数据 + authToken + 状态编辑）
  - Tab 2：历史遥测数据（ECharts 折线图 + 时间范围选择 + 分页表格）
  - Tab 3：操作日志（分页表格）
  - Tab 4：告警记录（该设备的历史告警列表）
- **新增 API**：`device.ts` 添加 `getDeviceHistory(deviceId, params)` / `getDeviceLogs(deviceId, params)`
- **路由**：`/devices/:deviceId` → `DeviceDetail`
- **修改文件**：`DeviceList.vue` — 详情按钮改为路由跳转（携带 deviceId）

#### 4.4 设备激活按钮 + 用户编辑
- **修改文件**：`DeviceList.vue`
  - 设备列表添加「激活」按钮（status=inactive 时显示），调用 `activateDevice(deviceId)`
- **修改文件**：`UserList.vue`
  - 添加「编辑」按钮，弹出编辑弹窗（修改 realName/email/phone）
- **后端修改**：`boonya-io-auth/.../controller/UserController.java` — `PUT /api/users/{id}` 支持更新基本信息字段（当前仅支持 status）

#### 4.5 404 路由 + 公共组件
- **修改文件**：`router/index.ts` — 添加 `/:pathMatch(.*)*` → 404 页面
- **新建文件**：`src/components/PageHeader.vue` — 统一页面头部（标题 + 操作按钮槽）
- **新建文件**：`src/components/SearchForm.vue` — 统一搜索表单容器

---

## 三、假设与决策

| # | 决策点 | 选择 | 理由 |
|---|--------|------|------|
| 1 | 告警表放哪个库 | `iot_device` 库（device 模块管理） | 告警与设备强关联，避免跨库 |
| 2 | iot 模块告警写入方式 | 直连 PostgreSQL 写入 `device_alert` 表 | 避免 HTTP 服务间调用复杂度，iot 已有 TDengine JDBC 可扩展 PG JDBC |
| 3 | 规则持久化放哪个库 | `iot_device` 库（与告警同库） | 规则与告警强关联，便于 JOIN 查询 |
| 4 | 权限映射方式 | 路径前缀+HTTP方法 → 权限code 配置表 | 可配置、可扩展，避免注解扫描复杂度 |
| 5 | 前端动态路由方案 | constantRoutes + asyncRoutes 动态注册 | Vue Router 标准方案，支持权限变化 |
| 6 | 按钮级权限控制 | 自定义 `v-permission` 指令 | 比 v-if + hasPermission 更简洁，可复用 |
| 7 | iot DeviceController 路径 | 改为 `/api/iot/devices` | 消除与 device 模块 `/api/devices` 冲突 |

---

## 四、验证步骤

### Phase 1 验证（Bug 修复）
1. 模拟 TDengine 断线→恢复→检查 pendingWrites 队列清空且数据写入成功
2. 非 admin 角色用户登录，token 中 roles 与 DB 一致
3. 网关访问 `/api/rules` 返回 200；`/api/iot/devices/{id}/history` 返回 200
4. MinIO 对含 `/` 的对象名调用 delete/exists/url 接口正常

### Phase 2 验证（告警闭环）
5. 触发温度超限告警 → `device_alert` 表有记录 → 前端历史告警列表可见
6. 告警状态流转：PENDING → ACKNOWLEDGED → RESOLVED → CLOSED 全流程
7. 重启 iot-core 后规则不丢失，从 DB 加载

### Phase 3 验证（RBAC）
8. admin 用户登录看到全部菜单；普通用户仅看到授权菜单
9. 无权限用户访问受限 API 返回 403
10. `v-permission` 指令正确隐藏无权限按钮

### Phase 4 验证（前端页面）
11. 告警规则管理页 CRUD 全流程
12. 角色管理页 CRUD + 权限分配
13. 设备详情页历史数据图表 + 日志表格 + 告警记录
14. 设备激活按钮功能正常
15. 用户编辑弹窗可修改基本信息
16. 访问不存在的路由跳转 404 页

### 最终整体验证
17. `docker compose up -d --build` 全量重建
18. 全部容器 healthy
19. 各 API 通过网关可访问
20. 前端页面功能完整可操作
