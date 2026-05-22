# Boonya IoT 管理后台

基于 Vue 3 + TypeScript + Element Plus 开发的 IoT 设备管理平台。

## 🚀 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **TypeScript** - JavaScript 的超集
- **Vite** - 下一代前端构建工具
- **Element Plus** - Vue 3 组件库
- **Pinia** - Vue 状态管理
- **Vue Router** - Vue 官方路由
- **Axios** - HTTP 客户端
- **ECharts** - 数据可视化图表库
- **MQTT.js** - MQTT 客户端

## 📦 安装依赖

```bash
npm install
```

## 🔧 开发

```bash
npm run dev
```

访问 http://localhost:3000

## 🏗️ 构建

```bash
npm run build
```

## 📁 项目结构

```
admin/
├── src/
│   ├── api/              # API 接口
│   │   ├── auth.ts       # 认证接口
│   │   └── device.ts     # 设备接口
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   ├── layouts/          # 布局组件
│   │   └── MainLayout.vue
│   ├── router/           # 路由配置
│   │   └── index.ts
│   ├── stores/           # Pinia 状态管理
│   │   └── user.ts
│   ├── utils/            # 工具函数
│   │   └── request.ts    # Axios 封装
│   ├── views/            # 页面视图
│   │   ├── Login.vue     # 登录页
│   │   ├── Dashboard.vue # 数据看板
│   │   ├── devices/      # 设备管理
│   │   ├── ota/          # OTA 管理
│   │   └── analytics/    # 数据分析
│   ├── App.vue
│   └── main.ts
├── index.html
├── package.json
├── vite.config.ts
└── tsconfig.json
```

## ✨ 核心功能

### 1. 用户认证
- ✅ JWT Token 登录
- ✅ 路由守卫
- ✅ Token 自动刷新
- ✅ 权限控制

### 2. 数据看板
- ✅ 设备统计卡片
- ✅ 实时数据趋势图
- ✅ 设备状态分布饼图

### 3. 设备管理
- ✅ 设备列表查询
- ✅ 设备注册
- ✅ 设备激活
- ✅ 设备编辑/删除
- ✅ 设备状态监控

### 4. 固件管理
- ✅ 固件上传
- ✅ 固件发布
- ✅ 固件版本管理

### 5. OTA 任务
- 🔄 任务创建
- 🔄 任务监控
- 🔄 进度跟踪

### 6. 数据分析
- 🔄 历史数据查询
- 🔄 数据导出
- 🔄 报表生成

## 🔌 API 配置

在 `vite.config.ts` 中配置代理：

```typescript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080', // 网关地址
      changeOrigin: true,
    },
  },
}
```

## 🎨 UI 组件

使用 Element Plus 组件库，已配置自动导入，无需手动引入。

```vue
<template>
  <el-button type="primary">按钮</el-button>
  <el-table :data="tableData">
    <!-- ... -->
  </el-table>
</template>
```

## 📱 响应式设计

支持桌面端和平板端访问，建议使用 Chrome、Firefox、Safari 等现代浏览器。

## 🔐 默认账号

- 用户名：`admin`
- 密码：`admin123`

## 📝 开发规范

### 命名规范
- 文件名：PascalCase（如 `DeviceList.vue`）
- 变量名：camelCase（如 `deviceList`）
- 常量名：UPPER_SNAKE_CASE（如 `MAX_COUNT`）

### 代码风格
- 使用 Composition API（`<script setup>`）
- 使用 TypeScript 类型注解
- 遵循 ESLint 规则

## 🚧 后续优化

1. 添加国际化支持
2. 实现主题切换
3. 添加单元测试
4. 优化性能（懒加载、虚拟滚动）
5. 完善错误处理
6. 添加埋点统计

## 📄 License

MIT
