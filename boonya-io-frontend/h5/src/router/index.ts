import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/devices',
    children: [
      {
        path: 'devices',
        name: 'Devices',
        component: () => import('@/views/DeviceList.vue'),
      },
      {
        path: 'device/:id',
        name: 'DeviceDetail',
        component: () => import('@/views/DeviceDetail.vue'),
      },
      {
        path: 'alerts',
        name: 'Alerts',
        component: () => import('@/views/AlertList.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!token && to.path !== '/login') {
    next('/login')
  } else if (token && to.path === '/login') {
    next('/')
  } else {
    next()
  }
})

export default router
