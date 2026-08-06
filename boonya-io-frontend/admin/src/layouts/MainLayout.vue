<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '200px'" class="layout-aside">
      <div class="logo">
        <span v-if="!isCollapse">Boonya IoT</span>
        <span v-else>IoT</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        router
      >
        <el-menu-item
          v-for="route in menuRoutes"
          :key="route.path"
          :index="route.path"
        >
          <el-icon><component :is="route.meta?.icon" /></el-icon>
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
        </div>
        
        <div class="header-right">
          <!-- 实时告警铃铛：订阅 MQTT alerts/# -->
          <el-badge :value="alertStore.unreadCount" :hidden="alertStore.unreadCount === 0" :max="99" class="alert-badge">
            <el-icon class="alert-bell" @click="router.push('/alerts')">
              <Bell />
            </el-icon>
          </el-badge>

          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ userStore.userInfo?.username || '管理员' }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
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

const menuRoutes = computed(() => {
  return router.options.routes.find(r => r.path === '/')?.children || []
})

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

// 订阅 MQTT alerts/#：收到告警写入 store + 弹通知
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
      // 用户取消或退出失败，都不处理
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

.layout-aside {
  background-color: #304156;
  transition: width 0.3s;
  overflow-x: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  background-color: #2b3a4c;
}

.el-menu {
  border-right: none;
  background-color: #304156;
}

:deep(.el-menu-item) {
  color: #bfcbd9;
}

:deep(.el-menu-item:hover),
:deep(.el-menu-item.is-active) {
  background-color: #263445 !important;
  color: #409eff !important;
}

.layout-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  transition: color 0.3s;
}

.collapse-btn:hover {
  color: #409eff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.alert-badge {
  margin-right: 4px;
}

.alert-bell {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
  transition: color 0.3s;
}

.alert-bell:hover {
  color: #e6a23c;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.layout-main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
