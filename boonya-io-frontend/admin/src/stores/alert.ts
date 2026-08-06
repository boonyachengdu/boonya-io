import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { RealtimeAlert } from '@/api/realtime'

// 全局告警 store：MainLayout 铃铛订阅 MQTT 写入，AlertList 页面读取
export const useAlertStore = defineStore('alert', () => {
  const alerts = ref<RealtimeAlert[]>([])
  const unreadCount = ref(0)

  const latest = computed(() => alerts.value[0])

  function pushAlert(alert: RealtimeAlert) {
    alerts.value.unshift(alert)
    if (alerts.value.length > 200) alerts.value.pop()
    unreadCount.value++
  }

  function markRead() {
    unreadCount.value = 0
  }

  function clear() {
    alerts.value = []
    unreadCount.value = 0
  }

  return { alerts, unreadCount, latest, pushAlert, markRead, clear }
})
