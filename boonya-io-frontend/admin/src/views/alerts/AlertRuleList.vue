<template>
  <div class="alert-rule-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>告警规则管理</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新增规则
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="规则名称">
          <el-input v-model="searchForm.ruleName" placeholder="请输入规则名称" clearable />
        </el-form-item>
        <el-form-item label="设备ID">
          <el-input v-model="searchForm.deviceId" placeholder="请输入设备ID" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 规则表格 -->
      <el-table :data="rules" border stripe v-loading="loading">
        <el-table-column prop="ruleName" label="规则名称" />
        <el-table-column prop="deviceId" label="设备ID" width="180">
          <template #default="{ row }">
            {{ row.deviceId || '全部设备' }}
          </template>
        </el-table-column>
        <el-table-column prop="metric" label="指标" width="120" />
        <el-table-column prop="operator" label="运算符" width="90" />
        <el-table-column prop="threshold" label="阈值" width="90" />
        <el-table-column label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="getSeverityType(row.severity)">{{ getSeverityText(row.severity) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled"
              @change="(val: boolean) => handleToggleEnabled(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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
        @size-change="loadRules"
        @current-change="loadRules"
      />
    </el-card>

    <!-- 新增规则弹窗 -->
    <el-dialog v-model="dialogVisible" title="新增告警规则" width="560px">
      <el-form ref="formRef" :model="createForm" :rules="formRules" label-width="100px">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="createForm.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="设备ID">
          <el-select
            v-model="createForm.deviceId"
            placeholder="留空表示全部设备"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option v-for="d in deviceOptions" :key="d.deviceId" :label="d.deviceName || d.deviceId" :value="d.deviceId" />
          </el-select>
        </el-form-item>
        <el-form-item label="指标" prop="metric">
          <el-select v-model="createForm.metric" placeholder="请选择指标" style="width: 100%">
            <el-option label="温度 (temp)" value="temp" />
            <el-option label="湿度 (humidity)" value="humidity" />
          </el-select>
        </el-form-item>
        <el-form-item label="运算符" prop="operator">
          <el-select v-model="createForm.operator" placeholder="请选择运算符" style="width: 100%">
            <el-option label="大于 (>)" value=">" />
            <el-option label="小于 (<)" value="<" />
            <el-option label="大于等于 (>=)" value=">=" />
            <el-option label="小于等于 (<=)" value="<=" />
            <el-option label="等于 (==)" value="==" />
            <el-option label="不等于 (!=)" value="!=" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值" prop="threshold">
          <el-input-number v-model="createForm.threshold" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="级别" prop="severity">
          <el-select v-model="createForm.severity" placeholder="请选择级别" style="width: 100%">
            <el-option label="提示 (INFO)" value="INFO" />
            <el-option label="警告 (WARNING)" value="WARNING" />
            <el-option label="严重 (CRITICAL)" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="冷却时间(ms)">
          <el-input-number v-model="createForm.cooldownMs" :min="0" :step="1000" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  getAlertRules,
  createAlertRule,
  enableAlertRule,
  disableAlertRule,
  deleteAlertRule,
} from '@/api/alert'
import type { AlertRule, AlertRuleCreateRequest, AlertSeverity, AlertOperator } from '@/api/alert'
import { getOnlineDevices } from '@/api/device'
import type { Device } from '@/api/device'

const loading = ref(false)
const rules = ref<AlertRule[]>([])
const deviceOptions = ref<Device[]>([])

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const searchForm = reactive({ ruleName: '', deviceId: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const createForm = reactive<AlertRuleCreateRequest>({
  ruleName: '',
  deviceId: '',
  metric: 'temp',
  operator: '>',
  threshold: 0,
  severity: 'WARNING',
  cooldownMs: 60000,
})

const formRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  metric: [{ required: true, message: '请选择指标', trigger: 'change' }],
  operator: [{ required: true, message: '请选择运算符', trigger: 'change' }],
  threshold: [{ required: true, message: '请输入阈值', trigger: 'blur' }],
  severity: [{ required: true, message: '请选择级别', trigger: 'change' }],
}

onMounted(() => {
  loadRules()
  loadDeviceOptions()
})

// 加载规则列表
const loadRules = async () => {
  loading.value = true
  try {
    const data = await getAlertRules({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ruleName: searchForm.ruleName || undefined,
      deviceId: searchForm.deviceId || undefined,
    })
    rules.value = data?.records || []
    pagination.total = data?.total ?? 0
  } catch (error) {
    console.error('Load alert rules error:', error)
  } finally {
    loading.value = false
  }
}

// 加载在线设备下拉选项
const loadDeviceOptions = async () => {
  try {
    deviceOptions.value = (await getOnlineDevices()) || []
  } catch (error) {
    console.error('Load online devices error:', error)
    deviceOptions.value = []
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadRules()
}

const handleReset = () => {
  searchForm.ruleName = ''
  searchForm.deviceId = ''
  pagination.pageNum = 1
  loadRules()
}

const handleCreate = () => {
  Object.assign(createForm, {
    ruleName: '',
    deviceId: '',
    metric: 'temp',
    operator: '>',
    threshold: 0,
    severity: 'WARNING',
    cooldownMs: 60000,
  })
  dialogVisible.value = true
}

const submitCreate = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await createAlertRule(createForm)
      ElMessage.success('创建成功')
      dialogVisible.value = false
      loadRules()
    } catch (error) {
      console.error('Create alert rule error:', error)
    }
  })
}

// 启用/禁用开关
const handleToggleEnabled = async (row: AlertRule, val: boolean) => {
  try {
    if (val) {
      await enableAlertRule(row.id)
      ElMessage.success('已启用')
    } else {
      await disableAlertRule(row.id)
      ElMessage.success('已禁用')
    }
    row.enabled = val
  } catch (error) {
    console.error('Toggle enabled error:', error)
  }
}

const handleDelete = async (row: AlertRule) => {
  try {
    await ElMessageBox.confirm(`确定要删除规则「${row.ruleName}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteAlertRule(row.id)
    ElMessage.success('删除成功')
    loadRules()
  } catch (error) {
    console.error('Delete alert rule error:', error)
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
</script>

<style scoped>
.alert-rule-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
