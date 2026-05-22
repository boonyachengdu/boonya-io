<template>
  <div class="dashboard">
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
            <el-icon class="stat-icon" color="#e6a23c"><Warning /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayAlerts }}</div>
              <div class="stat-label">今日告警</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon class="stat-icon" color="#f56c6c"><Upload /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.otaTasks }}</div>
              <div class="stat-label">OTA任务</div>
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
            <div class="card-header">
              <span>设备数据趋势</span>
            </div>
          </template>
          <div ref="chartRef" style="height: 400px"></div>
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
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'

const stats = ref({
  totalDevices: 128,
  onlineDevices: 96,
  todayAlerts: 5,
  otaTasks: 3,
})

const chartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
let chart: ECharts | null = null
let pieChart: ECharts | null = null

onMounted(() => {
  initChart()
  initPieChart()
})

const initChart = () => {
  if (!chartRef.value) return
  
  chart = echarts.init(chartRef.value)
  
  const option = {
    title: {
      text: '温度数据趋势',
      left: 'center',
    },
    tooltip: {
      trigger: 'axis',
    },
    legend: {
      data: ['温度', '湿度'],
      bottom: 10,
    },
    xAxis: {
      type: 'category',
      data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00'],
    },
    yAxis: {
      type: 'value',
    },
    series: [
      {
        name: '温度',
        type: 'line',
        data: [22, 21, 24, 28, 26, 24, 23],
        smooth: true,
      },
      {
        name: '湿度',
        type: 'line',
        data: [60, 62, 58, 55, 57, 61, 63],
        smooth: true,
      },
    ],
  }
  
  chart.setOption(option)
}

const initPieChart = () => {
  if (!pieChartRef.value) return
  
  pieChart = echarts.init(pieChartRef.value)
  
  const option = {
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
          { value: 96, name: '在线' },
          { value: 20, name: '离线' },
          { value: 8, name: '未激活' },
          { value: 4, name: '禁用' },
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
  }
  
  pieChart.setOption(option)
}
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

.card-header {
  font-weight: bold;
}
</style>
