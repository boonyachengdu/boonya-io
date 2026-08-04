<template>
  <div class="data-analytics">
    <el-card>
      <template #header>
        <div class="filter-bar">
          <span>数据分析</span>
          <div class="filters">
            <el-input
              v-model="deviceId"
              placeholder="请输入设备ID"
              style="width: 200px"
              clearable
              @keyup.enter="handleQuery"
            />
            <el-select v-model="period" style="width: 140px">
              <el-option label="近1小时" value="1h" />
              <el-option label="近6小时" value="6h" />
              <el-option label="近24小时" value="24h" />
              <el-option label="近7天" value="7d" />
            </el-select>
            <el-button type="primary" :loading="loading" @click="handleQuery">查询</el-button>
          </div>
        </div>
      </template>

      <div class="stat-row" v-loading="loading">
        <el-card v-for="item in statCards" :key="item.key" shadow="hover" class="stat-card">
          <el-statistic :title="item.title" :value="item.value" :precision="item.precision">
            <template #suffix>{{ item.suffix }}</template>
          </el-statistic>
        </el-card>
      </div>

      <el-card shadow="never" class="chart-card">
        <template #header>
          <span>温度趋势</span>
        </template>
        <div ref="chartRef" style="height: 400px"></div>
      </el-card>
    </el-card>
  </div>
</template>

<script setup lang="ts">
// 修改内容：修改人：pengjunlin 时间：2026-08-04 17:50:00 -- start ----
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { getDeviceRealtime, getDeviceTrend } from '@/api/analytics'
import type { DeviceRealtimeData, TrendPoint } from '@/api/analytics'

const deviceId = ref('sensor_1')
const period = ref('24h')
const loading = ref(false)
const realtime = ref<DeviceRealtimeData>({} as DeviceRealtimeData)
const trendData = ref<TrendPoint[]>([])

const chartRef = ref<HTMLElement>()
let chart: ECharts | null = null

const statCards = computed(() => [
  { key: 'latestTemp', title: '最新温度', value: realtime.value.latestTemp ?? 0, precision: 2, suffix: '°C' },
  { key: 'todayAvgTemp', title: '今日平均', value: realtime.value.todayAvgTemp ?? 0, precision: 2, suffix: '°C' },
  { key: 'todayMaxTemp', title: '今日最高', value: realtime.value.todayMaxTemp ?? 0, precision: 2, suffix: '°C' },
  { key: 'todayMinTemp', title: '今日最低', value: realtime.value.todayMinTemp ?? 0, precision: 2, suffix: '°C' },
  { key: 'dataPoints', title: '数据点数', value: realtime.value.dataPoints ?? 0, precision: 0, suffix: '' },
])

const loadRealtime = async () => {
  try {
    const data = await getDeviceRealtime(deviceId.value)
    realtime.value = data || ({} as DeviceRealtimeData)
  } catch (e) {
    realtime.value = {} as DeviceRealtimeData
  }
}

const loadTrend = async () => {
  try {
    const data = await getDeviceTrend(deviceId.value, period.value)
    trendData.value = Array.isArray(data) ? data : []
  } catch (e) {
    trendData.value = []
  }
}

const renderChart = () => {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  const option = {
    tooltip: {
      trigger: 'axis',
    },
    xAxis: {
      type: 'time',
    },
    yAxis: {
      type: 'value',
      name: '温度 (°C)',
    },
    series: [
      {
        name: '温度',
        type: 'line',
        smooth: true,
        data: trendData.value.map((p) => [p.timestamp, p.value]),
      },
    ],
  }
  chart.setOption(option, true)
}

const handleQuery = async () => {
  if (!deviceId.value) return
  loading.value = true
  try {
    await Promise.all([loadRealtime(), loadTrend()])
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

const handleResize = () => {
  chart?.resize()
}

onMounted(() => {
  handleQuery()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})
// 修改内容：修改人：pengjunlin 时间：2026-08-04 17:50:00 -- end ----
</script>

<style scoped>
.data-analytics {
  padding: 20px;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filters {
  display: flex;
  gap: 12px;
  align-items: center;
}

.stat-row {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  min-width: 180px;
}

.chart-card {
  margin-top: 20px;
}
</style>
