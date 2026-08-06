# 认证链路修复计划（Admin 端）

## 概述

修复 Admin 管理后台登录/退出/刷新/token 失效整条认证链路中的多个 Bug，使退出登录后能正常跳转到登录页，且退出后的 token 在服务端真正失效。

## 当前状态分析

### 已确认的 Bug 清单

| # | 严重度 | 位置 | 问题 |
|---|--------|------|------|
| B1 | 严重 | 后端 AuthController.logout | `@RequestParam String accessToken` 期望 query/form 参数，前端不传任何参数 → 400 Bad Request |
| B2 | 严重 | 后端 AuthController.refresh | `@RequestParam String refreshToken` 期望 query/form，前端发 JSON Body → 400 Bad Request |
| B3 | 严重 | 网关 AuthenticationFilter | 只校验 JWT 签名+过期，不查 Redis 黑名单 → 退出后 token 仍有效 |
| B4 | 严重 | 前端 MainLayout.vue handleCommand | 无 try/catch，logoutApi 失败时 router.push('/login') 被跳过 → 不跳转登录页 |
| B5 | 中 | 前端 stores/user.ts logout | try/finally 无 catch，错误向上抛，加剧 B4 |
| B6 | 中 | 后端 AuthService.logout | getClaimsFromToken 返回 null 时 NPE → 500 |
| B7 | 中 | 前后端 LoginResponse | 后端返回 `userId`，前端期望 `id`；前端期望 `roles` 数组，后端不返回 |
| B8 | 低 | 网关白名单 | `/api/auth/logout` 未加入白名单（退出时 token 可能已过期导致 401） |

### 调用链路（当前退出流程，标红为断点）

```
MainLayout 退出按钮
  → handleCommand (无 try/catch)
    → ElMessageBox.confirm
    → userStore.logout()
      → logoutApi() → POST /api/auth/logout (不带参数)
        → 网关校验 token (不查黑名单)
        → AuthController.logout(@RequestParam accessToken) ❌ 400 缺参数
      → finally: 清 localStorage token ✅
      → 错误向上抛 ❌
    → router.push('/login') ❌ 永不执行
结果: token 已清但页面不跳转，且后端黑名单未写入
```

## 修改方案

### 第一部分：后端修复（4 个文件）

#### 1. AuthController.java — 修复 logout 和 refresh 接口参数

**文件**: `e:\AI\java\boonya-io\boonya-io-auth\src\main\java\com\boonya\lab\io\auth\controller\AuthController.java`

**logout 接口改造**（B1）:
- 去掉 `@RequestParam String accessToken`
- 改为从 `Authorization` Header 提取 token（与网关一致用 `Bearer ` 前缀）
- 使用 `@RequestHeader(value = "Authorization", required = false) String authHeader`
- 内部截取 `Bearer ` 后的 token 传给 service

**refresh 接口改造**（B2）:
- 去掉 `@RequestParam String refreshToken`
- 改为 `@RequestBody Map<String, String> body`，从 `body.get("refreshToken")` 取值
- 或新建 `RefreshTokenRequest` DTO（与 LoginRequest 风格一致，推荐 DTO 方式）

#### 2. AuthService.java — 修复空指针 + 调整 logout 签名

**文件**: `e:\AI\java\boonya-io\boonya-io-auth\src\main\java\com\boonya\lab\io\auth\service\AuthService.java`

**logout 方法**（B6）:
- 接收 token 参数（从 Controller 传入，不再由 Service 直接接 RequestParam）
- 增加 null 判断：`Claims claims = jwtUtils.getClaimsFromToken(accessToken); if (claims == null) return;`
- 再取 `claims.getExpiration()`

#### 3. AuthenticationFilter.java — 加入 Redis 黑名单校验

**文件**: `e:\AI\java\boonya-io\boonya-io-gateway\src\main\java\com\boonya\lab\io\gateway\filter\AuthenticationFilter.java`

**黑名单校验**（B3）:
- 注入 `ReactiveRedisTemplate<String, String>`（gateway 已有 spring-boot-starter-data-redis-reactive 依赖）
- 在 JWT 签名校验通过后，检查 Redis key `auth:token:blacklist:{accessToken}` 是否存在
- 使用 `redisTemplate.hasKey(key)` 响应式查询
- 存在则返回 401（token 已失效）
- 注意：黑名单 key 的前缀要与 AuthService.logout 中写入的一致（`auth:token:blacklist:`）

**白名单补充**（B8）:
- `/api/auth/logout` 加入 `EXCLUDE_PATHS`（退出时 token 可能已过期，不应被网关拦截）

#### 4. LoginResponse.java — 字段名对齐

**文件**: `e:\AI\java\boonya-io\boonya-io-auth\src\main\java\com\boonya\lab\io\auth\dto\LoginResponse.java`

**UserInfo 字段调整**（B7）:
- `userId` → 改名为 `id`（与前端对齐），或用 `@JsonProperty("id")` 别名
- 新增 `roles` 字段（`List<String>`），当前无角色表则返回 `["admin"]` 默认值
- 在 AuthService.login 构造 LoginResponse 时填充 roles

### 第二部分：前端修复（3 个文件）

#### 5. MainLayout.vue — 修复退出跳转

**文件**: `e:\AI\java\boonya-io\boonya-io-frontend\admin\src\layouts\MainLayout.vue`

**handleCommand 改造**（B4）:
```typescript
const handleCommand = async (command: string) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', { ... })
      await userStore.logout()
    } catch {
      // 用户取消或退出失败，都不处理
    } finally {
      // 无论成功失败都跳转登录页
      router.push('/login')
    }
  }
}
```
- 把 `router.push('/login')` 移到 `finally` 块，确保必定跳转
- `catch` 吞掉取消和异常

#### 6. stores/user.ts — 修复 logout 错误处理

**文件**: `e:\AI\java\boonya-io\boonya-io-frontend\admin\src\stores\user.ts`

**logout 方法改造**（B5）:
```typescript
async function logout() {
  try {
    await logoutApi()
  } catch (e) {
    // 后端 logout 失败不影响本地清理
  } finally {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
  }
}
```
- 加 `catch` 吞掉错误，不向上抛

#### 7. api/auth.ts — 对齐字段名

**文件**: `e:\AI\java\boonya-io\boonya-io-frontend\admin\src\api\auth.ts`

**LoginResponse 接口调整**（B7）:
- `userInfo.id` 保持（后端已改为返回 `id`）
- `userInfo.roles` 保持（后端已改为返回数组）
- logout/refresh API 调用方式不变（后端已调整为不需要前端传 accessToken 参数）

## 假设与决策

1. **logout 不传 token 参数**：后端从 Authorization Header 取 token，前端无需改 API 调用方式。这比前端传参更安全（token 不出现在 URL/body 日志中）。
2. **refresh 用 RequestBody**：与 login 接口风格一致（login 已用 `@RequestBody`），新建 `RefreshTokenRequest` DTO。
3. **黑名单用 ReactiveRedisTemplate**：网关是 WebFlux 响应式栈，必须用响应式 Redis 客户端，不能用阻塞的 RedisTemplate。
4. **roles 默认值**：当前无角色表，login 时返回 `["admin"]`，前端权限判断不会因 undefined 报错。后续有角色表再改。
5. **不修改 H5 端**：用户明确仅修 Admin。
6. **签名注释**：按用户最新要求，不加任何签名注释（user_profile 中的签名注释规则仅限 Qt 项目）。

## 验证步骤

### 1. 编译验证
```powershell
cd e:\AI\java\boonya-io
mvn clean package -DskipTests -pl boonya-io-auth,boonya-io-gateway -am
```

### 2. 重建容器
```powershell
docker compose up -d --build auth gateway
```

### 3. 功能验证（按顺序）

**验证登录**:
```powershell
# 登录获取 token
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body '{"username":"admin","password":"admin123"}' -ContentType "application/json"
$response.data.accessToken  # 应返回有效 JWT
$response.data.userInfo.id  # 应返回数字（不是 undefined）
$response.data.userInfo.roles  # 应返回 ["admin"]
```

**验证退出跳转**:
- 浏览器打开 http://localhost:8080 登录
- 点击右上角退出登录 → 确认
- 应立即跳转到登录页（不需手动刷新）

**验证退出后 token 失效**:
```powershell
# 用退出前的 token 访问受保护接口
$headers = @{ Authorization = "Bearer $oldToken" }
Invoke-RestMethod -Uri "http://localhost:8080/api/devices" -Headers $headers
# 应返回 401（token 已在黑名单中）
```

**验证 refresh 接口**:
```powershell
# 用 refreshToken 刷新
$body = @{ refreshToken = $refreshToken } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/auth/refresh" -Method Post -Body $body -ContentType "application/json"
# 应返回新的 accessToken 和 refreshToken
```

**验证 401 自动跳转**:
- 登录后手动清除 localStorage 中的 token（或等 token 过期）
- 触发任意请求 → 应自动跳转登录页
