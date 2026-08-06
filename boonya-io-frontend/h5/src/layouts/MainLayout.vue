<template>
  <div class="layout">
    <router-view />

    <!-- 底部导航 -->
    <van-tabbar v-model="active" route>
      <van-tabbar-item icon="wap-home" to="/devices">设备</van-tabbar-item>
      <van-tabbar-item icon="bell" to="/alerts" :badge="alertStore.unreadCount > 0 ? alertStore.unreadCount : undefined">
        告警
      </van-tabbar-item>
      <van-tabbar-item icon="user" to="/devices">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { showToast } from 'vant'
import { useAlertStore } from '@/stores/alert'
import { subscribe } from '@/composables/useMqtt'
import { TOPIC_ALERTS, type RealtimeAlert } from '@/api/realtime'

const active = ref(0)
const alertStore = useAlertStore()

// 订阅 MQTT alerts/#：收到告警写入 store + Toast 提示
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
    showToast({
      type: 'fail',
      message: `${alert.deviceId}：${alert.message}`,
      duration: 3000,
    })
  })
})
onUnmounted(() => {
  unsubscribeAlerts?.()
})
</script>

<style scoped>
.layout {
  min-height: 100vh;
  padding-bottom: 50px;
}
</style>
