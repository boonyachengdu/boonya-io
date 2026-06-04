<template>
  <div class="energy-dashboard">
    <div class="page-title">
      <div>
        <h2>能碳管理</h2>
        <p>{{ overview.siteName }} · {{ overview.period }}</p>
      </div>
      <el-segmented v-model="period" :options="periodOptions" @change="loadTrend" />
    </div>

    <el-row :gutter="16">
      <el-col v-for="card in metricCards" :key="card.label" :xs="24" :sm="12" :lg="6">
        <el-card shadow="never" class="metric-card">
          <div class="metric-label">{{ card.label }}</div>
          <div class="metric-value">{{ card.value }}</div>
          <div class="metric-subtitle">{{ card.subtitle }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-row">
      <el-col :xs="24" :lg="15">
        <el-card shadow="never">
          <template #header>
            <span>能源趋势</span>
          </template>
          <div ref="trendChartRef" class="chart"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="9">
        <el-card shadow="never">
          <template #header>
            <span>区域能耗排行</span>
          </template>
          <el-table :data="areaRanking" height="360">
            <el-table-column prop="name" label="区域" min-width="110" />
            <el-table-column label="电量(kWh)" width="110">
              <template #default="{ row }">{{ formatNumber(row.electricityKwh) }}</template>
            </el-table-column>
            <el-table-column label="碳排(t)" width="90">
              <template #default="{ row }">{{ row.carbonTons }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-row">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never">
          <template #header>
            <span>能源设备状态</span>
          </template>
          <el-table :data="devices" height="330">
            <el-table-column prop="deviceName" label="设备" min-width="150" />
            <el-table-column prop="location" label="位置" min-width="130" />
            <el-table-column label="当前值" width="120">
              <template #default="{ row }">{{ row.value }} {{ row.unit }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card shadow="never">
          <template #header>
            <span>能耗告警</span>
          </template>
          <div class="alarm-list">
            <div v-for="alarm in alarms" :key="alarm.title" class="alarm-item">
              <el-tag :type="alarmTag(alarm.level)" size="small">{{ alarmLevelText(alarm.level) }}</el-tag>
              <div class="alarm-content">
                <div class="alarm-title">{{ alarm.title }}</div>
                <div class="alarm-desc">{{ alarm.description }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import {
  getAreaRanking,
  getEnergyAlarms,
  getEnergyDeviceStatus,
  getEnergyOverview,
  getEnergyTrend,
  type AreaRankingItem,
  type EnergyAlarm,
  type EnergyDeviceStatus,
  type EnergyOverview,
  type EnergyTrendPoint,
} from '@/api/energy'

const overview = ref<EnergyOverview>({
  siteName: 'Boonya 工业园区',
  period: '',
  electricityKwh: 0,
  waterM3: 0,
  solarKwh: 0,
  storageDischargeKwh: 0,
  energyCostCny: 0,
  carbonTons: 0,
  carbonReductionTons: 0,
  activeAlarms: 0,
  onlineDevices: 0,
  totalDevices: 0,
})

const trend = ref<EnergyTrendPoint[]>([])
const areaRanking = ref<AreaRankingItem[]>([])
const devices = ref<EnergyDeviceStatus[]>([])
const alarms = ref<EnergyAlarm[]>([])
const period = ref('day')
const periodOptions = [
  { label: '今日', value: 'day' },
  { label: '本月', value: 'month' },
  { label: '全年', value: 'year' },
]

const trendChartRef = ref<HTMLElement>()
let trendChart: ECharts | null = null

const metricCards = computed(() => [
  {
    label: '今日用电',
    value: `${formatNumber(overview.value.electricityKwh)} kWh`,
    subtitle: `在线 ${overview.value.onlineDevices}/${overview.value.totalDevices} 台`,
  },
  {
    label: '今日用水',
    value: `${formatNumber(overview.value.waterM3)} m3`,
    subtitle: '含生产与公共区域',
  },
  {
    label: '光伏发电',
    value: `${formatNumber(overview.value.solarKwh)} kWh`,
    subtitle: `减排 ${overview.value.carbonReductionTons} tCO2e`,
  },
  {
    label: '综合碳排',
    value: `${overview.value.carbonTons} tCO2e`,
    subtitle: `告警 ${overview.value.activeAlarms} 条`,
  },
])

onMounted(async () => {
  await loadData()
})

async function loadData() {
  const [overviewData, trendData, areas, deviceRows, alarmRows] = await Promise.all([
    getEnergyOverview(),
    getEnergyTrend(period.value),
    getAreaRanking(),
    getEnergyDeviceStatus(),
    getEnergyAlarms(),
  ])

  overview.value = overviewData
  trend.value = trendData
  areaRanking.value = areas
  devices.value = deviceRows
  alarms.value = alarmRows

  await nextTick()
  renderTrendChart()
}

async function loadTrend() {
  trend.value = await getEnergyTrend(period.value)
  renderTrendChart()
}

function renderTrendChart() {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { left: 48, right: 24, top: 24, bottom: 56 },
    xAxis: {
      type: 'category',
      data: trend.value.map((item) => item.time),
    },
    yAxis: [
      { type: 'value', name: 'kWh' },
      { type: 'value', name: 'm3 / t' },
    ],
    series: [
      {
        name: '用电',
        type: 'line',
        smooth: true,
        data: trend.value.map((item) => item.electricityKwh),
      },
      {
        name: '光伏',
        type: 'bar',
        data: trend.value.map((item) => item.solarKwh),
      },
      {
        name: '用水',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: trend.value.map((item) => item.waterM3),
      },
      {
        name: '碳排',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: trend.value.map((item) => item.carbonTons),
      },
    ],
  })
}

function formatNumber(value: number) {
  return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 1 })
}

function statusText(status: EnergyDeviceStatus['status']) {
  return { online: '在线', offline: '离线', warning: '告警' }[status]
}

function statusTag(status: EnergyDeviceStatus['status']) {
  return { online: 'success', offline: 'info', warning: 'warning' }[status]
}

function alarmLevelText(level: EnergyAlarm['level']) {
  return { high: '高', medium: '中', low: '低' }[level]
}

function alarmTag(level: EnergyAlarm['level']) {
  return { high: 'danger', medium: 'warning', low: 'info' }[level]
}
</script>

<style scoped>
.energy-dashboard {
  min-width: 0;
}

.page-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-title h2 {
  margin: 0;
  font-size: 22px;
  color: #1f2937;
}

.page-title p {
  margin: 6px 0 0;
  color: #6b7280;
}

.metric-card {
  margin-bottom: 16px;
}

.metric-label {
  color: #6b7280;
  font-size: 13px;
}

.metric-value {
  margin-top: 10px;
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

.metric-subtitle {
  margin-top: 8px;
  color: #9ca3af;
  font-size: 12px;
}

.content-row {
  margin-top: 16px;
}

.chart {
  height: 360px;
}

.alarm-list {
  height: 330px;
  overflow: auto;
}

.alarm-item {
  display: flex;
  gap: 10px;
  padding: 13px 0;
  border-bottom: 1px solid #edf0f5;
}

.alarm-item:last-child {
  border-bottom: none;
}

.alarm-content {
  min-width: 0;
}

.alarm-title {
  color: #1f2937;
  font-weight: 600;
}

.alarm-desc {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}
</style>
