<template>
  <div class="device-detail">
    <!-- 顶部返回 -->
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="goBack">返回设备列表</el-button>
      <span class="device-title">设备详情 - {{ device?.deviceName || deviceId }}</span>
    </div>

    <el-card v-loading="loading">
      <el-tabs v-model="activeTab">
        <!-- Tab 1：设备信息 -->
        <el-tab-pane label="设备信息" name="info">
          <el-descriptions :column="2" border v-if="device">
            <el-descriptions-item label="设备ID">{{ device.deviceId }}</el-descriptions-item>
            <el-descriptions-item label="设备名称">{{ device.deviceName }}</el-descriptions-item>
            <el-descriptions-item label="设备类型">{{ device.deviceType }}</el-descriptions-item>
            <el-descriptions-item label="设备型号">{{ device.model || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusType(device.status)">{{ getStatusText(device.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="固件版本">{{ device.firmwareVersion || '-' }}</el-descriptions-item>
            <el-descriptions-item label="Auth Token">{{ device.authToken || '-' }}</el-descriptions-item>
            <el-descriptions-item label="设备位置">{{ device.location || '-' }}</el-descriptions-item>
            <el-descriptions-item label="心跳时间">{{ device.lastHeartbeat || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ device.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ device.description || '-' }}</el-descriptions-item>
          </el-descriptions>

          <!-- 状态编辑 -->
          <div class="status-edit">
            <span class="label">修改状态：</span>
            <el-select v-model="editStatus" placeholder="请选择状态" style="width: 160px">
              <el-option label="在线" value="online" />
              <el-option label="离线" value="offline" />
              <el-option label="未激活" value="inactive" />
              <el-option label="禁用" value="disabled" />
            </el-select>
            <el-button type="primary" @click="submitStatus">保存</el-button>
          </div>
        </el-tab-pane>

        <!-- Tab 2：历史数据 -->
        <el-tab-pane label="历史数据" name="history">
          <div class="history-toolbar">
            <span>温度趋势</span>
            <el-radio-group v-model="timeRange" @change="loadHistory">
              <el-radio-button label="1h">最近1小时</el-radio-button>
              <el-radio-button label="6h">最近6小时</el-radio-button>
              <el-radio-button label="24h">最近24小时</el-radio-button>
            </el-radio-group>
          </div>
          <v-chart class="chart" :option="chartOption" autoresize v-loading="historyLoading" />
          <el-table :data="historyPoints" border stripe style="margin-top: 16px">
            <el-table-column label="时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.timestamp) }}
              </template>
            </el-table-column>
            <el-table-column label="温度(℃)">
              <template #default="{ row }">
                {{ row.temp ?? row.value ?? '-' }}
              </template>
            </el-table-column>
            <el-table-column label="湿度(%)">
              <template #default="{ row }">
                {{ row.humidity ?? '-' }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 3：操作日志 -->
        <el-tab-pane label="操作日志" name="logs">
          <el-table :data="logs" border stripe v-loading="logsLoading">
            <el-table-column prop="createTime" label="时间" width="180" />
            <el-table-column prop="action" label="操作" width="140">
              <template #default="{ row }">{{ row.action || row.operation || '-' }}</template>
            </el-table-column>
            <el-table-column prop="operator" label="操作人" width="140" />
            <el-table-column prop="detail" label="详情" show-overflow-tooltip />
          </el-table>
          <el-pagination
            v-model:current-page="logPagination.pageNum"
            v-model:page-size="logPagination.pageSize"
            :total="logPagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            style="margin-top: 20px; justify-content: flex-end"
            @size-change="loadLogs"
            @current-change="loadLogs"
          />
        </el-tab-pane>

        <!-- Tab 4：告警记录 -->
        <el-tab-pane label="告警记录" name="alerts">
          <el-table :data="deviceAlerts" border stripe v-loading="alertsLoading">
            <el-table-column label="告警标题" show-overflow-tooltip>
              <template #default="{ row }">{{ row.title || row.message || '-' }}</template>
            </el-table-column>
            <el-table-column label="级别" width="100">
              <template #default="{ row }">
                <el-tag :type="getSeverityType(row.severity)">{{ getSeverityText(row.severity) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getAlertStatusType(row.status)">{{ getAlertStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="triggerTime" label="触发时间" width="180" />
          </el-table>
          <el-pagination
            v-model:current-page="alertPagination.pageNum"
            v-model:page-size="alertPagination.pageSize"
            :total="alertPagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            style="margin-top: 20px; justify-content: flex-end"
            @size-change="loadDeviceAlerts"
            @current-change="loadDeviceAlerts"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  getDeviceByDeviceId,
  getDeviceHistory,
  getDeviceLogs,
  updateDeviceStatusByDeviceId,
} from '@/api/device'
import type { Device, DeviceHistoryPoint, DeviceLog } from '@/api/device'
import { getAlertList } from '@/api/alert'
import type { AlertItem, AlertSeverity, AlertStatus } from '@/api/alert'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import type { ComposeOption } from 'echarts/core'
import type { LineSeriesOption } from 'echarts/charts'
import type { GridComponentOption, TooltipComponentOption, LegendComponentOption, TitleComponentOption } from 'echarts/components'

// 注册 ECharts 模块
use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

type ECOption = ComposeOption<
  LineSeriesOption | GridComponentOption | TooltipComponentOption | LegendComponentOption | TitleComponentOption
>

const route = useRoute()
const router = useRouter()
const deviceId = route.params.deviceId as string

const activeTab = ref('info')
const loading = ref(false)
const device = ref<Device | null>(null)
const editStatus = ref('')

const goBack = () => {
  router.push('/devices')
}

// ===== Tab 1：设备信息 =====
const loadDevice = async () => {
  loading.value = true
  try {
    device.value = await getDeviceByDeviceId(deviceId)
    editStatus.value = device.value?.status || ''
  } catch (error) {
    console.error('Load device error:', error)
  } finally {
    loading.value = false
  }
}

const submitStatus = async () => {
  if (!editStatus.value) {
    ElMessage.warning('请选择状态')
    return
  }
  try {
    await updateDeviceStatusByDeviceId(deviceId, editStatus.value)
    ElMessage.success('状态更新成功')
    loadDevice()
  } catch (error) {
    console.error('Update status error:', error)
  }
}

// ===== Tab 2：历史数据 =====
const historyLoading = ref(false)
const historyPoints = ref<DeviceHistoryPoint[]>([])
const timeRange = ref('24h')

const loadHistory = async () => {
  historyLoading.value = true
  try {
    const data = await getDeviceHistory(deviceId, timeRange.value)
    historyPoints.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('Load history error:', error)
    historyPoints.value = []
  } finally {
    historyLoading.value = false
  }
}

// 折线图配置
const chartOption = computed<ECOption>(() => {
  const points = historyPoints.value
  return {
    title: { text: '温度数据趋势', left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['温度'], bottom: 10 },
    xAxis: {
      type: 'category',
      data: points.map((p) => formatTime(p.timestamp)),
    },
    yAxis: { type: 'value', name: '温度 (°C)' },
    series: [
      {
        name: '温度',
        type: 'line',
        data: points.map((p) => p.temp ?? p.value ?? 0),
        smooth: true,
      },
    ],
  }
})

// ===== Tab 3：操作日志 =====
const logsLoading = ref(false)
const logs = ref<DeviceLog[]>([])
const logPagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const loadLogs = async () => {
  logsLoading.value = true
  try {
    const data = await getDeviceLogs(deviceId, {
      pageNum: logPagination.pageNum,
      pageSize: logPagination.pageSize,
    })
    logs.value = data?.records || []
    logPagination.total = data?.total ?? 0
  } catch (error) {
    console.error('Load logs error:', error)
  } finally {
    logsLoading.value = false
  }
}

// ===== Tab 4：告警记录 =====
const alertsLoading = ref(false)
const deviceAlerts = ref<AlertItem[]>([])
const alertPagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const loadDeviceAlerts = async () => {
  alertsLoading.value = true
  try {
    const data = await getAlertList({
      pageNum: alertPagination.pageNum,
      pageSize: alertPagination.pageSize,
      deviceId,
    })
    deviceAlerts.value = data?.records || []
    alertPagination.total = data?.total ?? 0
  } catch (error) {
    console.error('Load device alerts error:', error)
  } finally {
    alertsLoading.value = false
  }
}

// ===== 工具方法 =====
function formatTime(ts: number): string {
  if (!ts) return '-'
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const getStatusType = (status: string) => {
  const types: Record<string, string> = {
    online: 'success',
    offline: 'info',
    inactive: 'warning',
    disabled: 'danger',
  }
  return types[status] || 'info'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    online: '在线',
    offline: '离线',
    inactive: '未激活',
    disabled: '禁用',
  }
  return texts[status] || status
}

// 级别 tag 颜色：WARNING->warning, CRITICAL->danger, INFO->info
const getSeverityType = (severity: AlertSeverity) => {
  const types: Record<AlertSeverity, string> = { INFO: 'info', WARNING: 'warning', CRITICAL: 'danger' }
  return types[severity] || 'info'
}

const getSeverityText = (severity: AlertSeverity) => {
  const texts: Record<AlertSeverity, string> = { INFO: '提示', WARNING: '警告', CRITICAL: '严重' }
  return texts[severity] || severity
}

// 告警状态 tag 颜色：PENDING->warning, ACKNOWLEDGED->primary, RESOLVED->success, CLOSED->info
const getAlertStatusType = (status: AlertStatus) => {
  const types: Record<AlertStatus, string> = {
    PENDING: 'warning',
    ACKNOWLEDGED: 'primary',
    RESOLVED: 'success',
    CLOSED: 'info',
  }
  return types[status] || 'info'
}

const getAlertStatusText = (status: AlertStatus) => {
  const texts: Record<AlertStatus, string> = {
    PENDING: '待处理',
    ACKNOWLEDGED: '已确认',
    RESOLVED: '已解决',
    CLOSED: '已关闭',
  }
  return texts[status] || status
}

onMounted(() => {
  loadDevice()
  loadHistory()
  loadLogs()
  loadDeviceAlerts()
})
</script>

<style scoped>
.device-detail {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.device-title {
  font-size: 16px;
  font-weight: bold;
}

.status-edit {
  margin-top: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-edit .label {
  font-weight: bold;
}

.history-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.chart {
  height: 360px;
  width: 100%;
}
</style>
