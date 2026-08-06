<template>
  <div class="ai-analysis">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>AI 智能分析</span>
        </div>
      </template>

      <!-- 设备选择 + 操作 -->
      <el-form :inline="true">
        <el-form-item label="选择设备">
          <el-select
            v-model="selectedDeviceId"
            filterable
            placeholder="请选择设备"
            style="width: 280px"
            @change="onDeviceChange"
          >
            <el-option
              v-for="d in devices"
              :key="d.deviceId"
              :label="`${d.deviceName} (${d.deviceId})`"
              :value="d.deviceId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="预测时长">
          <el-select v-model="predictMinutes" style="width: 120px">
            <el-option label="30分钟" :value="30" />
            <el-option label="60分钟" :value="60" />
            <el-option label="120分钟" :value="120" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="runAnalysis">开始分析</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 诊断报告 -->
    <el-card v-if="diagnosis" style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span>设备异常诊断</span>
          <el-tag :type="getDiagnosisStatusType(diagnosis.status)" size="large">
            {{ getDiagnosisStatusText(diagnosis.status) }}
          </el-tag>
        </div>
      </template>

      <el-alert
        v-if="diagnosis.status === 'NO_DATA'"
        :title="diagnosis.message"
        type="warning"
        :closable="false"
        show-icon
      />

      <template v-else>
        <!-- 统计指标 -->
        <el-descriptions :column="3" border title="统计指标（最近24小时）">
          <el-descriptions-item label="数据点数">{{ diagnosis.statistics?.count }}</el-descriptions-item>
          <el-descriptions-item label="均值">{{ diagnosis.statistics?.mean }}℃</el-descriptions-item>
          <el-descriptions-item label="标准差">{{ diagnosis.statistics?.std }}</el-descriptions-item>
          <el-descriptions-item label="最大值">{{ diagnosis.statistics?.max }}℃</el-descriptions-item>
          <el-descriptions-item label="最小值">{{ diagnosis.statistics?.min }}℃</el-descriptions-item>
          <el-descriptions-item label="极差">{{ diagnosis.statistics?.range }}℃</el-descriptions-item>
        </el-descriptions>

        <!-- 异常列表 -->
        <el-divider content-position="left">异常检测（{{ diagnosis.anomalyCount }} 项）</el-divider>
        <el-empty v-if="diagnosis.anomalyCount === 0" description="未检测到异常" :image-size="60" />
        <el-alert
          v-for="(item, idx) in diagnosis.anomalies"
          :key="idx"
          :title="item"
          type="error"
          :closable="false"
          show-icon
          style="margin-bottom: 8px"
        />

        <!-- 建议 -->
        <el-divider content-position="left">智能建议</el-divider>
        <el-alert
          v-for="(item, idx) in diagnosis.suggestions"
          :key="idx"
          :title="item"
          :type="diagnosis.anomalyCount === 0 ? 'success' : 'warning'"
          :closable="false"
          show-icon
          style="margin-bottom: 8px"
        />
      </template>
    </el-card>

    <!-- 趋势预测 -->
    <el-card v-if="prediction" style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span>温度趋势预测（{{ prediction.predictMinutes }} 分钟）</span>
          <el-tag :type="getTrendType(prediction.trend)" size="large">
            {{ getTrendText(prediction.trend) }}
          </el-tag>
        </div>
      </template>

      <el-alert
        v-if="prediction.status === 'NO_DATA'"
        :title="prediction.message"
        type="warning"
        :closable="false"
        show-icon
      />

      <template v-else>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="当前温度">{{ prediction.currentValue }}℃</el-descriptions-item>
          <el-descriptions-item label="预测温度">{{ prediction.predicted?.value }}℃</el-descriptions-item>
          <el-descriptions-item label="预测下界">{{ prediction.predicted?.lowerBound }}℃</el-descriptions-item>
          <el-descriptions-item label="预测上界">{{ prediction.predicted?.upperBound }}℃</el-descriptions-item>
          <el-descriptions-item label="变化斜率">{{ prediction.slope }}</el-descriptions-item>
          <el-descriptions-item label="预测时长">{{ prediction.predictMinutes }} 分钟</el-descriptions-item>
        </el-descriptions>

        <!-- 风险提示 -->
        <el-divider content-position="left">风险提示</el-divider>
        <el-empty v-if="prediction.risks.length === 0" description="无风险" :image-size="60" />
        <el-alert
          v-for="(item, idx) in prediction.risks"
          :key="idx"
          :title="item"
          type="error"
          :closable="false"
          show-icon
          style="margin-bottom: 8px"
        />
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getOnlineDevices } from '@/api/device'
import { diagnoseDevice, predictTrend } from '@/api/ai'
import type { Device } from '@/api/device'
import type { DeviceDiagnosis, TrendPrediction } from '@/api/ai'

const devices = ref<Device[]>([])
const selectedDeviceId = ref('')
const predictMinutes = ref(60)
const loading = ref(false)

const diagnosis = ref<DeviceDiagnosis | null>(null)
const prediction = ref<TrendPrediction | null>(null)

onMounted(async () => {
  try {
    devices.value = await getOnlineDevices()
  } catch (error) {
    console.error('Load devices error:', error)
  }
})

const onDeviceChange = () => {
  diagnosis.value = null
  prediction.value = null
}

const runAnalysis = async () => {
  if (!selectedDeviceId.value) {
    ElMessage.warning('请选择设备')
    return
  }
  loading.value = true
  try {
    const [diag, pred] = await Promise.all([
      diagnoseDevice(selectedDeviceId.value),
      predictTrend(selectedDeviceId.value, predictMinutes.value),
    ])
    diagnosis.value = diag
    prediction.value = pred
    ElMessage.success('分析完成')
  } catch (error) {
    console.error('AI analysis error:', error)
  } finally {
    loading.value = false
  }
}

const getDiagnosisStatusType = (status: string) => {
  const types: Record<string, string> = { NORMAL: 'success', ABNORMAL: 'danger', NO_DATA: 'info' }
  return types[status] || 'info'
}

const getDiagnosisStatusText = (status: string) => {
  const texts: Record<string, string> = { NORMAL: '正常', ABNORMAL: '异常', NO_DATA: '无数据' }
  return texts[status] || status
}

const getTrendType = (trend: string) => {
  const types: Record<string, string> = { STABLE: 'info', RISING: 'danger', FALLING: 'warning' }
  return types[trend] || 'info'
}

const getTrendText = (trend: string) => {
  const texts: Record<string, string> = { STABLE: '平稳', RISING: '上升', FALLING: '下降' }
  return texts[trend] || trend
}
</script>

<style scoped>
.ai-analysis {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
