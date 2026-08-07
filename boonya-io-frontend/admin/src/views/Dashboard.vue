<template>
  <div class="dashboard" v-loading="loading">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title">
        <h2>数据看板</h2>
        <p>实时设备监控与数据概览</p>
      </div>
    </div>

    <!-- 渐变统计卡片 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <div class="stat-card stat-blue">
          <div class="stat-card-body">
            <div class="stat-icon-wrap">
              <el-icon class="stat-icon"><Monitor /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalDevices }}</div>
              <div class="stat-label">设备总数</div>
            </div>
          </div>
          <div class="stat-card-bg"></div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card stat-green">
          <div class="stat-card-body">
            <div class="stat-icon-wrap">
              <el-icon class="stat-icon"><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.onlineDevices }}</div>
              <div class="stat-label">在线设备</div>
            </div>
          </div>
          <div class="stat-card-bg"></div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card stat-cyan">
          <div class="stat-card-body">
            <div class="stat-icon-wrap">
              <el-icon class="stat-icon"><Upload /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatNumber(stats.todayDataPoints) }}</div>
              <div class="stat-label">今日数据点</div>
            </div>
          </div>
          <div class="stat-card-bg"></div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card stat-orange">
          <div class="stat-card-body">
            <div class="stat-icon-wrap">
              <el-icon class="stat-icon"><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ offlineDevices }}</div>
              <div class="stat-label">离线设备</div>
            </div>
          </div>
          <div class="stat-card-bg"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon class="header-icon"><TrendCharts /></el-icon>
                设备温度趋势
              </span>
              <el-select
                v-model="selectedDeviceId"
                placeholder="请选择设备"
                style="width: 180px"
                @change="loadTrend"
              >
                <el-option
                  v-for="item in deviceOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </div>
          </template>
          <div ref="chartRef" style="height: 380px" v-loading="trendLoading"></div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon class="header-icon"><PieChart /></el-icon>
                设备状态分布
              </span>
            </div>
          </template>
          <div ref="pieChartRef" style="height: 380px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { getOverview, getDeviceTrend } from '@/api/analytics'
import type { OverviewData, TrendPoint } from '@/api/analytics'
import { getOnlineDevices } from '@/api/device'
import { subscribe } from '@/composables/useMqtt'
import { topicDeviceTelemetry, type RealtimeTelemetry } from '@/api/realtime'

const loading = ref(false)
const trendLoading = ref(false)

const stats = ref<OverviewData>({
  totalDevices: 0,
  onlineDevices: 0,
  todayDataPoints: 0,
})

const offlineDevices = computed(() =>
  Math.max(0, stats.value.totalDevices - stats.value.onlineDevices),
)

const selectedDeviceId = ref('')
const deviceOptions = ref<{ label: string; value: string }[]>([])

const formatNumber = (n: number) => {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

const loadDeviceOptions = async () => {
  try {
    const devices = await getOnlineDevices()
    const list = devices || []
    deviceOptions.value = list.map((d) => ({
      label: d.deviceName || d.deviceId,
      value: d.deviceId,
    }))
    if (deviceOptions.value.length > 0) {
      selectedDeviceId.value = deviceOptions.value[0].value
    }
  } catch (error) {
    console.error('Load online devices error:', error)
    deviceOptions.value = []
  }
}

const chartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
let chart: ECharts | null = null
let pieChart: ECharts | null = null

const formatTime = (ts: number) => {
  const d = new Date(ts)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

const loadOverview = async () => {
  loading.value = true
  try {
    const data = await getOverview()
    stats.value = {
      totalDevices: data?.totalDevices ?? 0,
      onlineDevices: data?.onlineDevices ?? 0,
      todayDataPoints: data?.todayDataPoints ?? 0,
    }
  } catch (error) {
    console.error('Load overview error:', error)
    stats.value = { totalDevices: 0, onlineDevices: 0, todayDataPoints: 0 }
  } finally {
    loading.value = false
  }
}

const trendPoints = ref<TrendPoint[]>([])

const loadTrend = async () => {
  if (!chart) return
  trendLoading.value = true
  try {
    const data = await getDeviceTrend(selectedDeviceId.value, '24h')
    const points: TrendPoint[] = Array.isArray(data) ? data : []
    trendPoints.value = points
    const xData = points.map((p) => formatTime(p.timestamp))
    const yData = points.map((p) => p.value)
    chart.setOption({
      xAxis: { data: xData },
      series: [{ name: '温度', data: yData }],
    })
  } catch (error) {
    console.error('Load trend error:', error)
    trendPoints.value = []
    chart.setOption({
      xAxis: { data: [] },
      series: [{ name: '温度', data: [] }],
    })
  } finally {
    trendLoading.value = false
  }
}

let unsubscribeTelemetry: (() => void) | null = null

const subscribeTelemetry = () => {
  unsubscribeTelemetry?.()
  unsubscribeTelemetry = null
  if (!selectedDeviceId.value) return

  unsubscribeTelemetry = subscribe(
    topicDeviceTelemetry(selectedDeviceId.value),
    (_topic, payload: RealtimeTelemetry) => {
      const temp = Number(payload?.temp ?? payload?.value ?? 0)
      const ts = Number(payload?.ts ?? payload?.timestamp ?? Date.now())
      trendPoints.value.push({ timestamp: ts, value: temp })
      if (trendPoints.value.length > 200) trendPoints.value.shift()
      if (!chart) return
      chart.setOption({
        xAxis: { data: trendPoints.value.map((p) => formatTime(p.timestamp)) },
        series: [{ name: '温度', data: trendPoints.value.map((p) => p.value) }],
      })
    },
  )
}

watch(selectedDeviceId, () => {
  subscribeTelemetry()
})

const initChart = () => {
  if (!chartRef.value) return

  chart = echarts.init(chartRef.value)

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(13, 40, 71, 0.9)',
      borderColor: 'rgba(0, 212, 255, 0.3)',
      borderWidth: 1,
      textStyle: { color: '#fff', fontSize: 13 },
      formatter: (params: any) => {
        const p = params[0]
        return `<div style="font-weight:600;margin-bottom:4px">${p.axisValue}</div>温度: <span style="color:#00d4ff;font-weight:600">${p.value}°C</span>`
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '8%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: [],
      axisLine: { lineStyle: { color: '#e0e4ea' } },
      axisLabel: { color: '#8492a6', fontSize: 11 },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      name: '温度 (°C)',
      nameTextStyle: { color: '#8492a6', fontSize: 12 },
      axisLine: { show: false },
      axisLabel: { color: '#8492a6' },
      splitLine: { lineStyle: { color: '#f0f2f5', type: 'dashed' } },
    },
    series: [
      {
        name: '温度',
        type: 'line',
        data: [],
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2.5, color: '#00d4ff' },
        itemStyle: { color: '#00d4ff', borderColor: '#fff', borderWidth: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0, 212, 255, 0.25)' },
            { offset: 1, color: 'rgba(0, 212, 255, 0.02)' },
          ]),
        },
      },
    ],
  })
}

const initPieChart = () => {
  if (!pieChartRef.value) return

  pieChart = echarts.init(pieChartRef.value)

  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(13, 40, 71, 0.9)',
      borderColor: 'rgba(0, 212, 255, 0.3)',
      borderWidth: 1,
      textStyle: { color: '#fff', fontSize: 13 },
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      bottom: 10,
      textStyle: { color: '#8492a6' },
    },
    series: [
      {
        name: '设备状态',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '45%'],
        data: [
          { value: stats.value.onlineDevices, name: '在线', itemStyle: { color: '#36cfc9' } },
          { value: offlineDevices.value, name: '离线', itemStyle: { color: '#ffd666' } },
        ],
        label: { color: '#5a5e66' },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.3)',
          },
        },
      },
    ],
  })
}

const updatePieChart = () => {
  if (!pieChart) return
  pieChart.setOption({
    series: [
      {
        data: [
          { value: stats.value.onlineDevices, name: '在线', itemStyle: { color: '#36cfc9' } },
          { value: offlineDevices.value, name: '离线', itemStyle: { color: '#ffd666' } },
        ],
      },
    ],
  })
}

onMounted(async () => {
  initChart()
  initPieChart()
  await loadOverview()
  updatePieChart()
  await loadDeviceOptions()
  if (selectedDeviceId.value) {
    await loadTrend()
    subscribeTelemetry()
  }
})

onUnmounted(() => {
  unsubscribeTelemetry?.()
  unsubscribeTelemetry = null
  chart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  padding: 4px;
}

/* 页面标题 */
.page-header {
  margin-bottom: 20px;
}

.page-title h2 {
  font-size: 22px;
  font-weight: 700;
  color: #0a1929;
  margin-bottom: 4px;
}

.page-title p {
  font-size: 13px;
  color: #8492a6;
}

/* 渐变统计卡片 */
.stat-card {
  position: relative;
  border-radius: 14px;
  padding: 24px;
  overflow: hidden;
  color: #fff;
  transition: transform 0.3s, box-shadow 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.stat-card-body {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}

.stat-icon {
  font-size: 30px;
  color: #fff;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  opacity: 0.85;
  margin-top: 4px;
}

/* 卡片渐变色 */
.stat-blue {
  background: linear-gradient(135deg, #1a4a7a 0%, #0d2847 100%);
}
.stat-green {
  background: linear-gradient(135deg, #0e7a5f 0%, #0a4d3a 100%);
}
.stat-cyan {
  background: linear-gradient(135deg, #0e6e8c 0%, #0a4a5e 100%);
}
.stat-orange {
  background: linear-gradient(135deg, #b8702a 0%, #8a4e1c 100%);
}

/* 装饰圆 */
.stat-card-bg {
  position: absolute;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  right: -30px;
  top: -30px;
}

/* 图表卡片 */
.chart-card {
  border-radius: 14px;
  border: none;
  box-shadow: 0 1px 3px rgba(0, 21, 41, 0.04);
}

:deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f2f5;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-icon {
  font-size: 18px;
  color: #00a0c8;
}
</style>
