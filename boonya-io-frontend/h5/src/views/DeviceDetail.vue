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
        <van-cell title="今日平均温度" :value="formatTemp(realtimeData.todayAvgTemp)" />
        <van-cell title="今日最高温度" :value="formatTemp(realtimeData.todayMaxTemp)" />
        <van-cell title="今日最低温度" :value="formatTemp(realtimeData.todayMinTemp)" />
        <van-cell title="数据点数" :value="realtimeData.dataPoints != null ? String(realtimeData.dataPoints) : '--'" />
      </template>
      <van-cell v-else title="暂无实时数据" />
    </van-cell-group>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:20:00 -- start ----
import { getDeviceById } from '@/api/device'
import { getDeviceRealtime } from '@/api/analytics'
import type { Device } from '@/api/device'
import type { DeviceRealtimeData } from '@/api/analytics'
// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:20:00 -- end ----

const route = useRoute()
// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:20:00 -- start ----
const deviceInfo = ref<Device | null>(null)
// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:20:00 -- end ----

onMounted(async () => {
  try {
    // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:20:00 -- start ----
    deviceInfo.value = await getDeviceById(Number(route.params.id))
    // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:20:00 -- end ----
  } catch (error) {
    console.error(error)
  }
})

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

// 修改内容：修改人：pengjunlin 时间：2026-08-04 17:50:00 -- start ----
const showRealtime = ref(false)
const realtimeLoading = ref(false)
// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:20:00 -- start ----
const realtimeData = ref<DeviceRealtimeData | null>(null)
// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:20:00 -- end ----

const formatTemp = (val?: number) => {
  if (val == null) return '--'
  return `${val.toFixed(2)} °C`
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
    // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:20:00 -- start ----
    realtimeData.value = await getDeviceRealtime(deviceId)
    // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:20:00 -- end ----
  } catch (error) {
    console.error(error)
    realtimeData.value = null
  } finally {
    realtimeLoading.value = false
  }
}
// 修改内容：修改人：pengjunlin 时间：2026-08-04 17:50:00 -- end ----
</script>

<style scoped>
.device-detail {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 16px;
}
</style>
