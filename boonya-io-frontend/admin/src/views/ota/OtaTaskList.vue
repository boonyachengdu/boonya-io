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

      <!-- 搜索栏：deviceId 可选筛选 + status 下拉筛选 -->
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="设备ID">
          <el-input
            v-model="searchForm.deviceId"
            placeholder="请输入设备ID（可选）"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 160px">
            <el-option label="待执行" value="pending" />
            <el-option label="下载中" value="downloading" />
            <el-option label="安装中" value="installing" />
            <el-option label="成功" value="success" />
            <el-option label="失败" value="failed" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 全局任务列表 + 分页 -->
      <el-table :data="tasks" border stripe v-loading="loading">
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

      <!-- 分页组件 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @size-change="loadTasks"
        @current-change="loadTasks"
      />
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
import { createOtaTask, queryOtaTasks, cancelOtaTask } from '@/api/ota'
import type { OtaTask } from '@/api/ota'
import { getFirmwareList } from '@/api/firmware'
import type { Firmware } from '@/api/firmware'

// OTA 任务管理页面：全局分页列表 + 可选筛选 + 创建/取消任务
const tasks = ref<OtaTask[]>([])
const firmwareOptions = ref<Firmware[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const creating = ref(false)

const searchForm = reactive({
  deviceId: '',
  status: '',
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0,
})

const createForm = reactive({
  deviceId: '',
  firmwareId: undefined as number | undefined,
})

onMounted(() => {
  loadFirmwareOptions()
  loadTasks() // 进入页面默认加载全部任务分页列表
})

const loadFirmwareOptions = async () => {
  try {
    // 已发布固件作为下拉选项，不分页（数据量不会太大）
    const page = await getFirmwareList({ pageNum: 1, pageSize: 200, status: 'published' })
    firmwareOptions.value = page?.records || []
  } catch (error) {
    console.error('Load firmware options error:', error)
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadTasks()
}

const handleReset = () => {
  searchForm.deviceId = ''
  searchForm.status = ''
  pagination.page = 1
  loadTasks()
}

const loadTasks = async () => {
  loading.value = true
  try {
    const page = await queryOtaTasks({
      pageNum: pagination.page,
      pageSize: pagination.size,
      deviceId: searchForm.deviceId || undefined,
      status: searchForm.status || undefined,
    })
    tasks.value = page?.records || []
    pagination.total = Number(page?.total ?? 0)
  } catch (error) {
    console.error('Load ota tasks error:', error)
    tasks.value = []
    pagination.total = 0
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
    // 新创建任务后刷新当前列表即可看到
    loadTasks()
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
