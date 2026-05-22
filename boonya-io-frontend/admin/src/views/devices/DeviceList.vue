<template>
  <div class="device-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>设备管理</span>
          <el-button type="primary" @click="handleRegister">
            <el-icon><Plus /></el-icon>
            注册设备
          </el-button>
        </div>
      </template>
      
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="设备名称">
          <el-input v-model="searchForm.deviceName" placeholder="请输入设备名称" />
        </el-form-item>
        <el-form-item label="设备状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态">
            <el-option label="全部" value="" />
            <el-option label="在线" value="online" />
            <el-option label="离线" value="offline" />
            <el-option label="未激活" value="inactive" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadDevices">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 表格 -->
      <el-table :data="devices" border stripe>
        <el-table-column prop="deviceId" label="设备ID" width="180" />
        <el-table-column prop="deviceName" label="设备名称" />
        <el-table-column prop="deviceType" label="设备类型" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">
              详情
            </el-button>
            <el-button link type="primary" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @size-change="loadDevices"
        @current-change="loadDevices"
      />
    </el-card>
    
    <!-- 注册设备对话框 -->
    <el-dialog v-model="dialogVisible" title="注册设备" width="500px">
      <el-form :model="registerForm" label-width="100px">
        <el-form-item label="设备名称" required>
          <el-input v-model="registerForm.deviceName" placeholder="请输入设备名称" />
        </el-form-item>
        <el-form-item label="设备类型" required>
          <el-select v-model="registerForm.deviceType" placeholder="请选择设备类型">
            <el-option label="传感器" value="sensor" />
            <el-option label="执行器" value="actuator" />
            <el-option label="网关" value="gateway" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="registerForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入设备描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRegister">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeviceList, registerDevice, deleteDevice } from '@/api/device'
import type { Device, DeviceRegisterRequest } from '@/api/device'

const devices = ref<Device[]>([])
const dialogVisible = ref(false)

const searchForm = reactive({
  deviceName: '',
  status: '',
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

const registerForm = reactive<DeviceRegisterRequest>({
  deviceName: '',
  deviceType: '',
  description: '',
})

onMounted(() => {
  loadDevices()
})

const loadDevices = async () => {
  try {
    const data = await getDeviceList({
      page: pagination.page,
      size: pagination.size,
    })
    devices.value = data.records
    pagination.total = data.total
  } catch (error) {
    console.error('Load devices error:', error)
  }
}

const handleRegister = () => {
  dialogVisible.value = true
  Object.assign(registerForm, {
    deviceName: '',
    deviceType: '',
    description: '',
  })
}

const submitRegister = async () => {
  if (!registerForm.deviceName || !registerForm.deviceType) {
    ElMessage.warning('请填写必填项')
    return
  }
  
  try {
    await registerDevice(registerForm)
    ElMessage.success('注册成功')
    dialogVisible.value = false
    loadDevices()
  } catch (error) {
    console.error('Register error:', error)
  }
}

const handleView = (row: Device) => {
  ElMessage.info(`查看设备：${row.deviceName}`)
}

const handleEdit = (row: Device) => {
  ElMessage.info(`编辑设备：${row.deviceName}`)
}

const handleDelete = async (row: Device) => {
  await ElMessageBox.confirm(`确定要删除设备 ${row.deviceName} 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  
  try {
    await deleteDevice(row.id)
    ElMessage.success('删除成功')
    loadDevices()
  } catch (error) {
    console.error('Delete error:', error)
  }
}

const handleReset = () => {
  searchForm.deviceName = ''
  searchForm.status = ''
  pagination.page = 1
  loadDevices()
}

const getStatusType = (status: string) => {
  const types: Record<string, any> = {
    online: 'success',
    offline: 'info',
    inactive: 'warning',
    disabled: 'danger',
  }
  return types[status] || 'info'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    online: '在线',
    offline: '离线',
    inactive: '未激活',
    disabled: '禁用',
  }
  return texts[status] || status
}
</script>

<style scoped>
.device-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
