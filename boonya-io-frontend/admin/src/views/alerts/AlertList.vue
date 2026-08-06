<template>
  <div class="alert-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>
            实时告警
            <el-tag v-if="alertStore.unreadCount > 0" type="danger" size="small" class="unread-tag">
              {{ alertStore.unreadCount }} 未读
            </el-tag>
          </span>
          <div>
            <el-button size="small" @click="alertStore.markRead()">标记已读</el-button>
            <el-button size="small" type="danger" plain @click="handleClear">清空</el-button>
          </div>
        </div>
      </template>

      <el-alert
        v-if="!alertStore.alerts.length"
        title="暂无告警"
        type="info"
        :closable="false"
        description="告警将通过 MQTT 实时推送，设备触发阈值规则后会在此显示。"
        show-icon
      />

      <el-table v-else :data="alertStore.alerts" border stripe>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.timestamp) }}
          </template>
        </el-table-column>
        <el-table-column prop="deviceId" label="设备ID" width="200" />
        <el-table-column label="温度(℃)" width="120">
          <template #default="{ row }">
            <span :class="{ 'temp-high': row.temp >= 50 }">{{ row.temp }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="告警内容" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useAlertStore } from '@/stores/alert'
import { ElMessageBox } from 'element-plus'

const alertStore = useAlertStore()

function formatTime(ts: number): string {
  if (!ts) return '-'
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('确定清空所有告警记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    alertStore.clear()
  } catch {
    // 取消
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.unread-tag {
  margin-left: 8px;
}

.temp-high {
  color: #f56c6c;
  font-weight: bold;
}
</style>
