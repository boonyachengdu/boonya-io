<template>
  <div class="device-detail">
    <van-nav-bar title="设备详情" left-arrow @click-left="$router.back()" />
    
    <van-cell-group inset>
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
      <van-button round block type="primary" icon="play-circle">
        查看实时数据
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/utils/request'

const route = useRoute()
const deviceInfo = ref<any>(null)

onMounted(async () => {
  try {
    deviceInfo.value = await request.get(`/devices/${route.params.id}`)
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
</script>

<style scoped>
.device-detail {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 16px;
}
</style>
