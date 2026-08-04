<template>
  <div class="dashboard" v-loading="loading">
    <!-- 修改内容：修改人：pengjunlin 时间：2026-08-04 17:30:00 -- start ---- -->
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
    <!-- 修改内容：修改人：pengjunlin 时间：2026-08-04 17:30:00 -- end ---- -->

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card>
          <template #header>
            <!-- 修改内容：修改人：pengjunlin 时间：2026-08-04 17:30:00 -- start ---- -->
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
            <!-- 修改内容：修改人：pengjunlin 时间：2026-08-04 17:30:00 -- end ---- -->
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
// 修改内容：修改人：pengjunlin 时间：2026-08-04 17:30:00 -- start ----
// 重写 Dashboard：移除 mock 数据，对接 getOverview / getDeviceTrend API
import { ref, computed, onMounted } from 'vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { getOverview, getDeviceTrend } from '@/api/analytics'
import type { OverviewData, TrendPoint } from '@/api/analytics'

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

// 设备ID选择下拉框，默认 sensor_1
const selectedDeviceId = ref('sensor_1')
const deviceOptions = ref([
  { label: 'sensor_1', value: 'sensor_1' },
  { label: 'sensor_2', value: 'sensor_2' },
  { label: 'sensor_3', value: 'sensor_3' },
])

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
const loadTrend = async () => {
  if (!chart) return
  trendLoading.value = true
  try {
    const data = await getDeviceTrend(selectedDeviceId.value, '24h')
    const points: TrendPoint[] = Array.isArray(data) ? data : []
    const xData = points.map((p) => formatTime(p.timestamp))
    const yData = points.map((p) => p.value)
    chart.setOption({
      xAxis: { data: xData },
      series: [{ name: '温度', data: yData }],
    })
  } catch (error) {
    console.error('Load trend error:', error)
    chart.setOption({
      xAxis: { data: [] },
      series: [{ name: '温度', data: [] }],
    })
  } finally {
    trendLoading.value = false
  }
}

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
  await loadTrend()
})
// 修改内容：修改人：pengjunlin 时间：2026-08-04 17:30:00 -- end ----
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

/* 修改内容：修改人：pengjunlin 时间：2026-08-04 17:30:00 -- start ---- */
/* card-header 改为 flex 布局以容纳设备ID下拉框 */
.card-header {
  font-weight: bold;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
/* 修改内容：修改人：pengjunlin 时间：2026-08-04 17:30:00 -- end ---- */
</style>
