<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo">
        <div class="logo-icon-mini">
          <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect x="3" y="3" width="26" height="26" rx="6" stroke="currentColor" stroke-width="2" opacity="0.4"/>
            <circle cx="16" cy="16" r="7" stroke="currentColor" stroke-width="2"/>
            <circle cx="16" cy="16" r="2.5" fill="currentColor"/>
          </svg>
        </div>
        <transition name="fade">
          <span v-if="!isCollapse" class="logo-text">Boonya IoT</span>
        </transition>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        router
        class="sidebar-menu"
      >
        <el-menu-item
          v-for="route in menuRoutes"
          :key="route.path"
          :index="route.path"
          class="menu-item"
        >
          <el-icon class="menu-icon"><component :is="route.meta?.icon" /></el-icon>
          <template #title>{{ route.meta?.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>

          <!-- 面包屑 -->
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <!-- 实时告警铃铛 -->
          <el-badge :value="alertStore.unreadCount" :hidden="alertStore.unreadCount === 0" :max="99" class="alert-badge">
            <el-icon class="alert-bell" @click="router.push('/alerts')">
              <Bell />
            </el-icon>
          </el-badge>

          <div class="header-divider"></div>

          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <div class="user-avatar">
                {{ (userStore.userInfo?.username || 'A').charAt(0).toUpperCase() }}
              </div>
              <span class="user-name">{{ userStore.userInfo?.username || '管理员' }}</span>
              <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主要内容 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox, ElNotification } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useAlertStore } from '@/stores/alert'
import { subscribe } from '@/composables/useMqtt'
import { TOPIC_ALERTS, type RealtimeAlert } from '@/api/realtime'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const alertStore = useAlertStore()

const isCollapse = ref(false)

const activeMenu = computed(() => route.path)

// 面包屑标题
const currentTitle = computed(() => {
  const matched = route.matched.filter(item => item.meta?.title)
  const last = matched[matched.length - 1]
  return last?.meta?.title || ''
})

const menuRoutes = computed(() => {
  const children = router.options.routes.find(r => r.path === '/')?.children || []
  return children.filter((r) => !r.meta?.hidden)
})

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

// 订阅 MQTT alerts/#
let unsubscribeAlerts: (() => void) | null = null
onMounted(() => {
  unsubscribeAlerts = subscribe(TOPIC_ALERTS, (topic, payload) => {
    const alert: RealtimeAlert = {
      message: payload?.message || '未知告警',
      deviceId: payload?.deviceId || topic.split('/')[1] || 'unknown',
      temp: Number(payload?.temp ?? payload?.value ?? 0),
      timestamp: Number(payload?.timestamp ?? payload?.ts ?? Date.now()),
    }
    alertStore.pushAlert(alert)
    ElNotification({
      title: '设备告警',
      message: `${alert.deviceId}：${alert.message}`,
      type: 'warning',
      duration: 5000,
    })
  })
})
onUnmounted(() => {
  unsubscribeAlerts?.()
})

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
      await userStore.logout()
    } catch {
      // 用户取消
    } finally {
      router.push('/login')
    }
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

/* ===== 侧边栏 ===== */
.layout-aside {
  background: linear-gradient(180deg, #0a1929 0%, #0d2847 100%);
  transition: width 0.3s ease;
  overflow-x: hidden;
  overflow-y: auto;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.logo-icon-mini {
  width: 32px;
  height: 32px;
  color: #00d4ff;
  flex-shrink: 0;
}

.logo-icon-mini svg {
  width: 100%;
  height: 100%;
}

.logo-text {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #ffffff 0%, #00d4ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  white-space: nowrap;
}

.sidebar-menu {
  border-right: none;
  background: transparent !important;
  padding: 8px 0;
}

:deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.65);
  margin: 2px 8px;
  border-radius: 8px;
  height: 46px;
  line-height: 46px;
  transition: all 0.25s;
}

:deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06) !important;
  color: #fff !important;
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.15) 0%, rgba(14, 165, 233, 0.1) 100%) !important;
  color: #00d4ff !important;
  box-shadow: inset 3px 0 0 #00d4ff;
}

.menu-icon {
  font-size: 18px;
}

/* ===== 顶部导航 ===== */
.layout-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  padding: 0 20px;
  height: 56px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #5a5e66;
  transition: color 0.3s;
  padding: 4px;
}

.collapse-btn:hover {
  color: #00d4ff;
}

.breadcrumb {
  font-size: 14px;
}

:deep(.el-breadcrumb__inner.is-link) {
  color: #97a8be;
  font-weight: 400;
}

:deep(.el-breadcrumb__inner) {
  color: #303133;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.alert-badge {
  margin-right: 4px;
}

.alert-bell {
  font-size: 20px;
  cursor: pointer;
  color: #5a5e66;
  transition: all 0.3s;
  padding: 4px;
  border-radius: 6px;
}

.alert-bell:hover {
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.08);
}

.header-divider {
  width: 1px;
  height: 24px;
  background: #e0e4ea;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.25s;
}

.user-info:hover {
  background: #f5f7fa;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0d2847 0%, #103a5c 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.dropdown-arrow {
  font-size: 12px;
  color: #97a8be;
}

/* ===== 主内容区 ===== */
.layout-main {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
