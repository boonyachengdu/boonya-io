# 分支合并计划：master + codex → dev → master

## 当前状态分析

### 分支拓扑

```
          07ba9cf (master) feat: P0-P2全量问题修复与架构演进
          6ccce53 (master) doc: 提交README.md
        |
        | 6b537dc (codex/...) codex: 代码分析提价
        | 78edf67 (codex/...) feat: add Boonya Energy IoT MVP
|/
* 6f97b44 (dev) fix: 设置docker和localhost 环境依赖  ← 共同祖先
```

- `dev` 停在共同祖先 `6f97b44`，没有任何额外提交
- `master` 在祖先之上有 2 个提交（README + P0-P2 修复），改 57 文件 +4361/-190
- `codex/boonya-energy-iot-mvp` 在祖先之上有 2 个提交（能碳 MVP + 代码分析），改 7 文件 +1160/-1

### 冲突分析（关键结论：无冲突）

| 文件 | master 是否修改 | codex 是否修改 | 冲突风险 |
|------|:---:|:---:|:---:|
| `admin/src/router/index.ts` | 否（相对祖先无变化） | 是（新增 energy 路由） | 无 |
| `.gitignore` | 否 | 是（新增 node_modules/dist 忽略） | 无 |
| `ENERGY_CARBON_COMMERCIALIZATION_PLAN.md` | 否 | 新增 | 无 |
| `EnergyAnalyticsController.java` | 否 | 新增 | 无 |
| `EnergyAnalyticsService.java` | 否 | 新增 | 无 |
| `admin/src/api/energy.ts` | 否 | 新增 | 无 |
| `admin/src/views/energy/EnergyDashboard.vue` | 否 | 新增 | 无 |
| 其余 56 个文件 | 是（P0-P2） | 否 | 无 |

**结论：两个分支修改的文件完全不重叠，合并将零冲突。**

## 合并步骤

### Step 1: 切换到 dev 分支
```bash
git checkout dev
```

### Step 2: 将 master 合并到 dev（Fast-Forward）
```bash
git merge master
```
因为 `dev` 就在 `master` 的祖先位置，这将是一个 fast-forward 合并，`dev` 直接指向 `07ba9cf`。

### Step 3: 将 codex/boonya-energy-iot-mvp 合并到 dev
```bash
git merge codex/boonya-energy-iot-mvp
```
预期结果：无冲突的干净合并。codex 分支新增的 7 个文件（能碳管理模块）会合入。

### Step 4: 验证合并结果
- 确认 `boonya-io-iot/src/main/java/.../controller/EnergyAnalyticsController.java` 存在
- 确认 `boonya-io-frontend/admin/src/views/energy/EnergyDashboard.vue` 存在
- 确认 `boonya-io-frontend/admin/src/router/index.ts` 同时包含 firmware/ota-tasks/analytics 和 energy 路由
- 确认 P0-P2 修改的文件（Dashboard.vue、FirmwareList.vue 等）仍在
- 确认 `.gitignore` 包含 node_modules/dist 规则
- 运行 `git log --oneline --graph -10` 确认提交历史

### Step 5: 将 dev 合并回 master（可选，用户确认后执行）
```bash
git checkout master
git merge dev
```
由于 dev 包含了 master 的所有提交 + codex 的新增内容，这将是 fast-forward 合并。

### Step 6: 推送到远程
```bash
git push origin dev
git push origin master
```

## 合并后 dev 分支预期文件结构

```
master 的全部内容（P0-P2 修复 + 架构演进 + K8s + 部署配置）
+ codex 的新增内容：
  ├── ENERGY_CARBON_COMMERCIALIZATION_PLAN.md          ← 能碳商业化计划文档
  ├── boonya-io-iot/.../EnergyAnalyticsController.java  ← 能碳分析后端接口
  ├── boonya-io-iot/.../EnergyAnalyticsService.java     ← 能碳分析服务
  ├── admin/src/api/energy.ts                           ← 能碳前端 API
  ├── admin/src/views/energy/EnergyDashboard.vue        ← 能碳管理页面
  └── admin/src/router/index.ts                         ← 新增 energy 路由
```

## 风险评估

- **冲突风险：零** — 两分支修改的文件完全不重叠
- **编译风险：低** — codex 新增的 EnergyAnalyticsController/Service 依赖 TimeSeriesService（已有），不引入新依赖
- **运行时风险：低** — energy 模块是独立新增，不影响现有功能
