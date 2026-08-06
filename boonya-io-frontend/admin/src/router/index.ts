import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据看板', icon: 'DataAnalysis' },
      },
      {
        path: 'devices',
        name: 'Devices',
        component: () => import('@/views/devices/DeviceList.vue'),
        meta: { title: '设备管理', icon: 'Monitor' },
      },
      {
        path: 'firmware',
        name: 'Firmware',
        component: () => import('@/views/ota/FirmwareList.vue'),
        meta: { title: '固件管理', icon: 'Upload' },
      },
      {
        path: 'ota-tasks',
        name: 'OtaTasks',
        component: () => import('@/views/ota/OtaTaskList.vue'),
        meta: { title: 'OTA任务', icon: 'Refresh' },
      },
      {
        path: 'analytics',
        name: 'Analytics',
        component: () => import('@/views/analytics/DataAnalytics.vue'),
        meta: { title: '数据分析', icon: 'TrendCharts' },
      },
      {
        path: 'alerts',
        name: 'Alerts',
        component: () => import('@/views/alerts/AlertList.vue'),
        meta: { title: '实时告警', icon: 'Bell' },
      },
      {
        path: 'energy',
        name: 'EnergyDashboard',
        component: () => import('@/views/energy/EnergyDashboard.vue'),
        meta: { title: '能碳管理', icon: 'Sunny' },
      },
      {
        path: 'system/users',
        name: 'UserList',
        component: () => import('@/views/system/UserList.vue'),
        meta: { title: '用户管理', icon: 'UserFilled' },
      },
      {
        path: 'ai-analysis',
        name: 'AiAnalysis',
        component: () => import('@/views/ai/AiAnalysis.vue'),
        meta: { title: 'AI分析', icon: 'Cpu' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
