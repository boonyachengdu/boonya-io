<template>
  <div class="ota-task-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>OTA 任务管理</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            创建任务
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="设备ID">
          <el-input
            v-model="searchForm.deviceId"
            placeholder="请输入设备ID"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>

      <!-- 任务列表 -->
      <el-table v-if="searchedDeviceId" :data="tasks" border stripe v-loading="loading">
        <el-table-column prop="id" label="任务ID" width="80" />
        <el-table-column prop="deviceId" label="设备ID" width="180" />
        <el-table-column prop="firmwareId" label="固件ID" width="100" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="180">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0" />
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" show-overflow-tooltip />
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="completeTime" label="完成时间" width="180" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canCancel(row.status)"
              link
              type="danger"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 初始空状态提示 -->
      <el-empty v-else description="请输入设备ID查询任务" />
    </el-card>

    <!-- 创建任务对话框 -->
    <el-dialog v-model="dialogVisible" title="创建 OTA 任务" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="设备ID" required>
          <el-input v-model="createForm.deviceId" placeholder="请输入设备ID" />
        </el-form-item>
        <el-form-item label="固件" required>
          <el-select
            v-model="createForm.firmwareId"
            placeholder="请选择已发布固件"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="fw in firmwareOptions"
              :key="fw.id"
              :label="`${fw.deviceModel} - v${fw.version}`"
              :value="fw.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createOtaTask, getDeviceOtaTasks, cancelOtaTask } from '@/api/ota'
import type { OtaTask } from '@/api/ota'
import { getFirmwareList } from '@/api/firmware'
import type { Firmware } from '@/api/firmware'

// 修改内容：修改人：pengjunlin 时间：2026-08-04 17:40:00 -- start ----
// 重写 OTA 任务管理页面，对接后端 OTA 任务 API：按设备查询任务、创建任务、取消任务
const tasks = ref<OtaTask[]>([])
const firmwareOptions = ref<Firmware[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const creating = ref(false)
const searchedDeviceId = ref('')

const searchForm = reactive({
  deviceId: '',
})

const createForm = reactive({
  deviceId: '',
  firmwareId: undefined as number | undefined,
})

onMounted(() => {
  loadFirmwareOptions()
})

const loadFirmwareOptions = async () => {
  try {
    const data = await getFirmwareList({ status: 'published' })
    firmwareOptions.value = data || []
  } catch (error) {
    console.error('Load firmware options error:', error)
  }
}

const handleSearch = async () => {
  if (!searchForm.deviceId) {
    ElMessage.warning('请输入设备ID')
    return
  }
  searchedDeviceId.value = searchForm.deviceId
  await loadTasks()
}

const loadTasks = async () => {
  if (!searchedDeviceId.value) return
  loading.value = true
  try {
    const data = await getDeviceOtaTasks(searchedDeviceId.value)
    tasks.value = data || []
  } catch (error) {
    console.error('Load ota tasks error:', error)
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  createForm.deviceId = ''
  createForm.firmwareId = undefined
  dialogVisible.value = true
}

const submitCreate = async () => {
  if (!createForm.deviceId) {
    ElMessage.warning('请输入设备ID')
    return
  }
  if (!createForm.firmwareId) {
    ElMessage.warning('请选择固件')
    return
  }

  creating.value = true
  try {
    await createOtaTask(createForm.deviceId, createForm.firmwareId)
    ElMessage.success('创建成功')
    dialogVisible.value = false
    // 如果当前查询的设备就是新创建任务的设备，刷新列表
    if (searchedDeviceId.value === createForm.deviceId) {
      loadTasks()
    }
  } catch (error) {
    console.error('Create ota task error:', error)
  } finally {
    creating.value = false
  }
}

const handleCancel = async (row: OtaTask) => {
  await ElMessageBox.confirm(`确定要取消任务 ${row.id} 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  try {
    await cancelOtaTask(row.id)
    ElMessage.success('取消成功')
    loadTasks()
  } catch (error) {
    console.error('Cancel ota task error:', error)
  }
}

const canCancel = (status: string) => {
  return ['pending', 'downloading', 'installing'].includes(status)
}

const getStatusType = (status: string) => {
  const types: Record<string, any> = {
    pending: 'info',
    downloading: 'primary',
    installing: 'warning',
    success: 'success',
    failed: 'danger',
    cancelled: 'info',
  }
  return types[status] || 'info'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    pending: '待执行',
    downloading: '下载中',
    installing: '安装中',
    success: '成功',
    failed: '失败',
    cancelled: '已取消',
  }
  return texts[status] || status
}
// 修改内容：修改人：pengjunlin 时间：2026-08-04 17:40:00 -- end ----
</script>

<style scoped>
.ota-task-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
