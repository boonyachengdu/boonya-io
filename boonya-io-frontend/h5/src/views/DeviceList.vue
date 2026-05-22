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
import request from '@/utils/request'

const router = useRouter()
const devices = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

const onLoad = async () => {
  try {
    const data = await request.get('/devices', { params: { page: 1, size: 20 } })
    devices.value = data.records
    loading.value = false
    finished.value = true
  } catch (error) {
    loading.value = false
  }
}

const onRefresh = async () => {
  finished.value = false
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
