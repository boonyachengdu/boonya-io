<template>
  <div class="device-list">
    <van-nav-bar title="设备列表" />
    
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list v-model:loading="loading" :finished="finished" @load="onLoad">
        <van-cell
          v-for="device in devices"
          :key="device.id"
          :title="device.deviceName"
          :label="`ID: ${device.deviceId}`"
          is-link
          @click="goToDetail(device.id)"
        >
          <template #right-icon>
            <van-tag :type="getStatusType(device.status)">
              {{ getStatusText(device.status) }}
            </van-tag>
          </template>
        </van-cell>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getDeviceList } from '@/api/device'
import type { Device, DeviceQueryParams } from '@/api/device'

const router = useRouter()
const devices = ref<Device[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

const page = ref(1)
const pageSize = 20

const onLoad = async () => {
  try {
    const data = await getDeviceList({ pageNum: page.value, pageSize })
    const records = data?.records || []
    if (records.length > 0) {
      devices.value.push(...records)
      page.value++
    }
    const total = data?.total
    if (records.length < pageSize || (typeof total === 'number' && devices.value.length >= total)) {
      finished.value = true
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const onRefresh = async () => {
  page.value = 1
  devices.value = []
  finished.value = false
  loading.value = true
  await onLoad()
  refreshing.value = false
}

const goToDetail = (id: number) => {
  router.push(`/device/${id}`)
}

const getStatusType = (status: string) => {
  const types: Record<string, any> = {
    online: 'success',
    offline: 'default',
    inactive: 'warning',
  }
  return types[status] || 'default'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    online: '在线',
    offline: '离线',
    inactive: '未激活',
  }
  return texts[status] || status
}
</script>

<style scoped>
.device-list {
  min-height: 100vh;
  background-color: #f5f5f5;
}
</style>
