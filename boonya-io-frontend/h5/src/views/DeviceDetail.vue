<template>
  <div class="device-detail">
    <van-nav-bar title="设备详情" left-arrow @click-left="$router.back()" />

    <van-cell-group inset title="基本信息">
      <van-cell title="设备名称" :value="deviceInfo?.deviceName" />
      <van-cell title="设备ID" :value="deviceInfo?.deviceId" />
      <van-cell title="设备类型" :value="deviceInfo?.deviceType" />
      <van-cell title="状态">
        <van-tag :type="getStatusType(deviceInfo?.status)">
          {{ getStatusText(deviceInfo?.status) }}
        </van-tag>
      </van-cell>
    </van-cell-group>

    <div style="margin: 16px;">
      <van-button round block type="primary" icon="play-circle" @click="toggleRealtime">
        {{ showRealtime ? '收起实时数据' : '查看实时数据' }}
      </van-button>
    </div>

    <van-cell-group v-if="showRealtime" inset title="实时数据">
      <div v-if="realtimeLoading" style="padding: 20px; text-align: center;">
        <van-loading type="spinner" size="24px">加载中...</van-loading>
      </div>
      <template v-else-if="realtimeData">
        <van-cell title="最新温度">
          <template #value>
            <van-tag type="primary">{{ formatTemp(realtimeData.latestTemp) }}</van-tag>
          </template>
        </van-cell>
        <van-cell title="更新时间" :value="formatTime(realtimeData.latestTimestamp)" />
        <van-cell title="今日平均温度" :value="formatTemp(realtimeData.todayAvgTemp)" />
        <van-cell title="今日最高温度" :value="formatTemp(realtimeData.todayMaxTemp)" />
        <van-cell title="今日最低温度" :value="formatTemp(realtimeData.todayMinTemp)" />
        <van-cell title="数据点数" :value="realtimeData.dataPoints != null ? String(realtimeData.dataPoints) : '--'" />
      </template>
      <van-cell v-else title="暂无实时数据" />
    </van-cell-group>

    <!-- MQTT 实时推送的最近温度点 -->
    <van-cell-group v-if="showRealtime && trendPoints.length" inset title="实时推送（最近）">
      <van-cell
        v-for="(p, idx) in trendPoints.slice().reverse()"
        :key="p.timestamp + '-' + idx"
        :title="formatTime(p.timestamp)"
      >
        <template #value>
          <van-tag :type="p.value >= 50 ? 'danger' : 'primary'">{{ p.value.toFixed(2) }} °C</van-tag>
        </template>
      </van-cell>
    </van-cell-group>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getDeviceById } from '@/api/device'
import { getDeviceRealtime } from '@/api/analytics'
import type { Device } from '@/api/device'
import type { DeviceRealtimeData } from '@/api/analytics'
import { subscribe } from '@/composables/useMqtt'
import { topicDeviceTelemetry, type RealtimeTelemetry } from '@/api/realtime'

const route = useRoute()
const deviceInfo = ref<Device | null>(null)

// MQTT 实时遥测订阅：device/{deviceId}/telemetry
let unsubscribeTelemetry: (() => void) | null = null

onMounted(async () => {
  try {
    deviceInfo.value = await getDeviceById(Number(route.params.id))
    // 设备信息就绪后自动订阅实时遥测
    subscribeTelemetry()
  } catch (error) {
    console.error(error)
  }
})

onUnmounted(() => {
  unsubscribeTelemetry?.()
  unsubscribeTelemetry = null
})

function subscribeTelemetry() {
  unsubscribeTelemetry?.()
  unsubscribeTelemetry = null
  const deviceId = deviceInfo.value?.deviceId
  if (!deviceId) return

  unsubscribeTelemetry = subscribe(
    topicDeviceTelemetry(deviceId),
    (_topic, payload: RealtimeTelemetry) => {
      const temp = Number(payload?.temp ?? payload?.value ?? 0)
      const ts = Number(payload?.ts ?? payload?.timestamp ?? Date.now())
      // 实时刷新最新温度
      if (!realtimeData.value) {
        realtimeData.value = { deviceId, latestTemp: temp, latestTimestamp: ts }
      } else {
        realtimeData.value = {
          ...realtimeData.value,
          latestTemp: temp,
          latestTimestamp: ts,
        }
      }
      // 追加到迷你趋势点
      trendPoints.value.push({ timestamp: ts, value: temp })
      if (trendPoints.value.length > 60) trendPoints.value.shift()
    },
  )
}

const getStatusType = (status?: string) => {
  if (!status) return 'default'
  const types: Record<string, any> = {
    online: 'success',
    offline: 'default',
    inactive: 'warning',
  }
  return types[status] || 'default'
}

const getStatusText = (status?: string) => {
  if (!status) return '未知'
  const texts: Record<string, string> = {
    online: '在线',
    offline: '离线',
    inactive: '未激活',
  }
  return texts[status] || status
}

const showRealtime = ref(false)
const realtimeLoading = ref(false)
const realtimeData = ref<DeviceRealtimeData | null>(null)
// MQTT 实时追加的迷你趋势点（最近 60 个）
const trendPoints = ref<{ timestamp: number; value: number }[]>([])

const formatTemp = (val?: number) => {
  if (val == null) return '--'
  return `${val.toFixed(2)} °C`
}

const formatTime = (ts?: number) => {
  if (!ts) return '--'
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const toggleRealtime = async () => {
  showRealtime.value = !showRealtime.value
  if (showRealtime.value && !realtimeData.value && deviceInfo.value?.deviceId) {
    await loadRealtime()
  }
}

const loadRealtime = async () => {
  const deviceId = deviceInfo.value?.deviceId
  if (!deviceId) return
  realtimeLoading.value = true
  try {
    realtimeData.value = await getDeviceRealtime(deviceId)
  } catch (error) {
    console.error(error)
    realtimeData.value = null
  } finally {
    realtimeLoading.value = false
  }
}
</script>

<style scoped>
.device-detail {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 16px;
}
</style>
