<template>
  <div class="dashboard" v-loading="loading">
    <!-- 统计卡片对接 getOverview API，离线设备由 totalDevices-onlineDevices 计算 -->
    <el-row :gutter="20">
      <!-- 统计卡片 -->
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon class="stat-icon" color="#409eff"><Monitor /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalDevices }}</div>
              <div class="stat-label">设备总数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon class="stat-icon" color="#67c23a"><CircleCheck /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.onlineDevices }}</div>
              <div class="stat-label">在线设备</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon class="stat-icon" color="#f56c6c"><Upload /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayDataPoints }}</div>
              <div class="stat-label">今日数据点</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon class="stat-icon" color="#e6a23c"><Warning /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ offlineDevices }}</div>
              <div class="stat-label">离线设备</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card>
          <template #header>
            <!-- 趋势图卡片头新增设备ID选择下拉框 -->
            <div class="card-header">
              <span>设备温度趋势</span>
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
          <div ref="chartRef" style="height: 400px" v-loading="trendLoading"></div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>设备状态分布</span>
            </div>
          </template>
          <div ref="pieChartRef" style="height: 400px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
// 重写 Dashboard：移除 mock 数据，对接 getOverview / getDeviceTrend API + MQTT 实时遥测追加
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

// 离线设备 = 设备总数 - 在线设备
const offlineDevices = computed(() =>
  Math.max(0, stats.value.totalDevices - stats.value.onlineDevices),
)

// 设备ID选择下拉框：动态加载在线设备，默认选中第一个
const selectedDeviceId = ref('')
const deviceOptions = ref<{ label: string; value: string }[]>([])

// 加载在线设备列表填充下拉
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

// 毫秒时间戳转换为 HH:mm
const formatTime = (ts: number) => {
  const d = new Date(ts)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

// 获取系统概览，失败时保持默认值 0
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

// 获取设备温度趋势，失败时清空图表
// 同时缓存当前趋势数据，用于 MQTT 实时点追加
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

// MQTT 实时遥测：选定设备后订阅 device/{id}/telemetry，追加最新点到趋势图
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
      // 仅保留最近 200 个点，避免无限增长
      if (trendPoints.value.length > 200) trendPoints.value.shift()
      if (!chart) return
      chart.setOption({
        xAxis: { data: trendPoints.value.map((p) => formatTime(p.timestamp)) },
        series: [{ name: '温度', data: trendPoints.value.map((p) => p.value) }],
      })
    },
  )
}

// 设备切换时重新加载趋势 + 重新订阅实时遥测
watch(selectedDeviceId, () => {
  subscribeTelemetry()
})

const initChart = () => {
  if (!chartRef.value) return

  chart = echarts.init(chartRef.value)

  chart.setOption({
    title: {
      text: '温度数据趋势',
      left: 'center',
    },
    tooltip: {
      trigger: 'axis',
    },
    legend: {
      data: ['温度'],
      bottom: 10,
    },
    xAxis: {
      type: 'category',
      data: [],
    },
    yAxis: {
      type: 'value',
      name: '温度 (°C)',
    },
    series: [
      {
        name: '温度',
        type: 'line',
        data: [],
        smooth: true,
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
    },
    legend: {
      bottom: 10,
    },
    series: [
      {
        name: '设备状态',
        type: 'pie',
        radius: '50%',
        data: [
          { value: stats.value.onlineDevices, name: '在线' },
          { value: offlineDevices.value, name: '离线' },
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
      },
    ],
  })
}

// 概览数据加载后刷新饼图（在线 vs 离线占比）
const updatePieChart = () => {
  if (!pieChart) return
  pieChart.setOption({
    series: [
      {
        data: [
          { value: stats.value.onlineDevices, name: '在线' },
          { value: offlineDevices.value, name: '离线' },
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
  // 先加载在线设备下拉，有选中设备时再加载趋势 + 订阅实时遥测
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
  padding: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  font-size: 48px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #999;
  margin-top: 4px;
}

/* card-header 改为 flex 布局以容纳设备ID下拉框 */
.card-header {
  font-weight: bold;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
