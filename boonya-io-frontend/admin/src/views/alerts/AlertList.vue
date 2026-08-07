<template>
  <div class="alert-list">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- Tab 1：实时告警（保留原有 MQTT 实时告警展示） -->
      <el-tab-pane label="实时告警" name="realtime">
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
      </el-tab-pane>

      <!-- Tab 2：历史告警 -->
      <el-tab-pane label="历史告警" name="history">
        <!-- 顶部统计卡片 -->
        <el-row :gutter="16" class="stat-row">
          <el-col :span="6">
            <el-card shadow="hover">
              <div class="stat-card">
                <el-icon class="stat-icon" color="#409eff"><Bell /></el-icon>
                <div class="stat-info">
                  <div class="stat-value">{{ statistics.todayTotal }}</div>
                  <div class="stat-label">今日总数</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <div class="stat-card">
                <el-icon class="stat-icon" color="#e6a23c"><Warning /></el-icon>
                <div class="stat-info">
                  <div class="stat-value">{{ statistics.pending }}</div>
                  <div class="stat-label">待处理</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <div class="stat-card">
                <el-icon class="stat-icon" color="#409eff"><CircleCheck /></el-icon>
                <div class="stat-info">
                  <div class="stat-value">{{ statistics.acknowledged }}</div>
                  <div class="stat-label">已确认</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <div class="stat-card">
                <el-icon class="stat-icon" color="#67c23a"><Select /></el-icon>
                <div class="stat-info">
                  <div class="stat-value">{{ statistics.resolved }}</div>
                  <div class="stat-label">已解决</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 筛选表单 -->
        <el-card style="margin-top: 16px">
          <el-form :inline="true" :model="searchForm">
            <el-form-item label="设备ID">
              <el-input v-model="searchForm.deviceId" placeholder="请输入设备ID" clearable />
            </el-form-item>
            <el-form-item label="严重级别">
              <el-select v-model="searchForm.severity" placeholder="请选择级别" clearable style="width: 140px">
                <el-option label="提示" value="INFO" />
                <el-option label="警告" value="WARNING" />
                <el-option label="严重" value="CRITICAL" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 140px">
                <el-option label="待处理" value="PENDING" />
                <el-option label="已确认" value="ACKNOWLEDGED" />
                <el-option label="已解决" value="RESOLVED" />
                <el-option label="已关闭" value="CLOSED" />
              </el-select>
            </el-form-item>
            <el-form-item label="时间范围">
              <el-date-picker
                v-model="searchForm.timeRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 360px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 历史告警表格 -->
          <el-table :data="historyAlerts" border stripe v-loading="historyLoading">
            <el-table-column prop="deviceId" label="设备ID" width="180" />
            <el-table-column label="告警标题" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.title || row.message || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="级别" width="100">
              <template #default="{ row }">
                <el-tag :type="getSeverityType(row.severity)">{{ getSeverityText(row.severity) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="triggerTime" label="触发时间" width="180" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 'PENDING'" link type="primary" @click="handleAcknowledge(row)">
                  确认
                </el-button>
                <el-button v-if="row.status === 'ACKNOWLEDGED'" link type="success" @click="handleResolve(row)">
                  解决
                </el-button>
                <el-button v-if="row.status === 'RESOLVED'" link type="info" @click="handleClose(row)">
                  关闭
                </el-button>
                <span v-if="row.status === 'CLOSED'" class="text-muted">已关闭</span>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            style="margin-top: 20px; justify-content: flex-end"
            @size-change="loadHistoryAlerts"
            @current-change="loadHistoryAlerts"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAlertStore } from '@/stores/alert'
import { ElMessageBox } from 'element-plus'
import {
  getAlertList,
  getAlertStatistics,
  acknowledgeAlert,
  resolveAlert,
  closeAlert,
} from '@/api/alert'
import type { AlertItem, AlertStatistics, AlertSeverity, AlertStatus } from '@/api/alert'

const alertStore = useAlertStore()
const activeTab = ref<'realtime' | 'history'>('realtime')

// ===== 历史告警相关状态 =====
const historyLoading = ref(false)
const historyAlerts = ref<AlertItem[]>([])
const statistics = ref<AlertStatistics>({ todayTotal: 0, pending: 0, acknowledged: 0, resolved: 0 })

const searchForm = reactive<{
  deviceId: string
  severity: AlertSeverity | ''
  status: AlertStatus | ''
  timeRange: [string, string] | null
}>({
  deviceId: '',
  severity: '',
  status: '',
  timeRange: null,
})

const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

onMounted(() => {
  loadStatistics()
  loadHistoryAlerts()
})

// 加载告警统计
const loadStatistics = async () => {
  try {
    const data = await getAlertStatistics()
    statistics.value = {
      todayTotal: data?.todayTotal ?? 0,
      pending: data?.pending ?? 0,
      acknowledged: data?.acknowledged ?? 0,
      resolved: data?.resolved ?? 0,
    }
  } catch (error) {
    console.error('Load alert statistics error:', error)
  }
}

// 加载历史告警列表
const loadHistoryAlerts = async () => {
  historyLoading.value = true
  try {
    const data = await getAlertList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      deviceId: searchForm.deviceId || undefined,
      severity: searchForm.severity || undefined,
      status: searchForm.status || undefined,
      startTime: searchForm.timeRange?.[0] || undefined,
      endTime: searchForm.timeRange?.[1] || undefined,
    })
    historyAlerts.value = data?.records || []
    pagination.total = data?.total ?? 0
  } catch (error) {
    console.error('Load history alerts error:', error)
  } finally {
    historyLoading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadHistoryAlerts()
}

const handleReset = () => {
  searchForm.deviceId = ''
  searchForm.severity = ''
  searchForm.status = ''
  searchForm.timeRange = null
  pagination.pageNum = 1
  loadHistoryAlerts()
}

// 确认告警（PENDING -> ACKNOWLEDGED）
const handleAcknowledge = async (row: AlertItem) => {
  try {
    await acknowledgeAlert(row.id)
    ElMessage.success('确认成功')
    loadHistoryAlerts()
    loadStatistics()
  } catch (error) {
    console.error('Acknowledge error:', error)
  }
}

// 解决告警（ACKNOWLEDGED -> RESOLVED）
const handleResolve = async (row: AlertItem) => {
  try {
    await resolveAlert(row.id)
    ElMessage.success('解决成功')
    loadHistoryAlerts()
    loadStatistics()
  } catch (error) {
    console.error('Resolve error:', error)
  }
}

// 关闭告警（RESOLVED -> CLOSED）
const handleClose = async (row: AlertItem) => {
  try {
    await closeAlert(row.id)
    ElMessage.success('关闭成功')
    loadHistoryAlerts()
    loadStatistics()
  } catch (error) {
    console.error('Close error:', error)
  }
}

// 时间格式化（实时告警时间戳）
function formatTime(ts: number): string {
  if (!ts) return '-'
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// 清空实时告警
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

// 级别 tag 颜色：WARNING->warning, CRITICAL->danger, INFO->info
const getSeverityType = (severity: AlertSeverity) => {
  const types: Record<AlertSeverity, string> = { INFO: 'info', WARNING: 'warning', CRITICAL: 'danger' }
  return types[severity] || 'info'
}

const getSeverityText = (severity: AlertSeverity) => {
  const texts: Record<AlertSeverity, string> = { INFO: '提示', WARNING: '警告', CRITICAL: '严重' }
  return texts[severity] || severity
}

// 状态 tag 颜色：PENDING->warning, ACKNOWLEDGED->primary, RESOLVED->success, CLOSED->info
const getStatusType = (status: AlertStatus) => {
  const types: Record<AlertStatus, string> = {
    PENDING: 'warning',
    ACKNOWLEDGED: 'primary',
    RESOLVED: 'success',
    CLOSED: 'info',
  }
  return types[status] || 'info'
}

const getStatusText = (status: AlertStatus) => {
  const texts: Record<AlertStatus, string> = {
    PENDING: '待处理',
    ACKNOWLEDGED: '已确认',
    RESOLVED: '已解决',
    CLOSED: '已关闭',
  }
  return texts[status] || status
}
</script>

<style scoped>
.alert-list {
  padding: 20px;
}

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

.stat-row {
  margin-bottom: 0;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  font-size: 40px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

.text-muted {
  color: #999;
}
</style>
