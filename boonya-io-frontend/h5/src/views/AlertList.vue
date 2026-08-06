<template>
  <div class="alert-list">
    <van-nav-bar title="实时告警">
      <template #right>
        <van-icon name="delete-o" size="18" @click="handleClear" v-if="alertStore.alerts.length" />
      </template>
    </van-nav-bar>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <div v-if="alertStore.unreadCount > 0" class="unread-bar" @click="alertStore.markRead()">
        <van-icon name="bell" />
        <span>{{ alertStore.unreadCount }} 条未读，点击标记已读</span>
      </div>

      <div v-if="!alertStore.alerts.length" class="empty">
        <van-empty description="暂无告警，MQTT 实时推送中" />
      </div>

      <van-cell-group v-else inset>
        <van-cell
          v-for="(alert, idx) in alertStore.alerts"
          :key="alert.timestamp + '-' + idx"
        >
          <template #title>
            <div class="alert-item">
              <div class="alert-head">
                <van-tag :type="alert.temp >= 50 ? 'danger' : 'warning'">
                  {{ alert.temp.toFixed(1) }}°C
                </van-tag>
                <span class="alert-device">{{ alert.deviceId }}</span>
              </div>
              <div class="alert-msg">{{ alert.message }}</div>
              <div class="alert-time">{{ formatTime(alert.timestamp) }}</div>
            </div>
          </template>
        </van-cell>
      </van-cell-group>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { showConfirmDialog } from 'vant'
import { useAlertStore } from '@/stores/alert'

const alertStore = useAlertStore()
const refreshing = ref(false)

function formatTime(ts: number): string {
  if (!ts) return '-'
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function onRefresh() {
  // 告警来自 MQTT 实时推送，下拉刷新仅重置状态
  refreshing.value = false
}

async function handleClear() {
  try {
    await showConfirmDialog({ title: '提示', message: '确定清空所有告警记录吗？' })
    alertStore.clear()
  } catch {
    // 取消
  }
}
</script>

<style scoped>
.alert-list {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 16px;
}

.unread-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: #fff7e8;
  color: #ed6a0c;
  font-size: 13px;
  cursor: pointer;
}

.empty {
  padding-top: 40px;
}

.alert-item {
  line-height: 1.6;
}

.alert-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.alert-device {
  font-weight: bold;
  font-size: 14px;
  color: #323233;
}

.alert-msg {
  font-size: 13px;
  color: #646566;
}

.alert-time {
  font-size: 12px;
  color: #969799;
  margin-top: 2px;
}
</style>
