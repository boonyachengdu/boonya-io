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
          <!-- 修改内容：修改人：pengjunlin 时间：2026-08-04 17:30:00 -- start ---- -->
          <!-- P0-6: 查询按钮改为 handleSearch，搜索时重置页码为 1 -->
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <!-- 修改内容：修改人：pengjunlin 时间：2026-08-04 17:30:00 -- end ---- -->
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

    <!-- 修改内容：修改人：pengjunlin 时间：2026-08-04 17:30:00 -- start ---- -->
    <!-- P0-5: 注册表单新增 deviceId（必填）与 location 字段 -->
    <!-- 注册设备对话框 -->
    <el-dialog v-model="dialogVisible" title="注册设备" width="500px">
      <el-form :model="registerForm" label-width="100px">
        <el-form-item label="设备ID" required>
          <el-input v-model="registerForm.deviceId" placeholder="请输入设备ID" />
        </el-form-item>
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
        <el-form-item label="设备型号">
          <el-input v-model="registerForm.model" placeholder="请输入设备型号" />
        </el-form-item>
        <el-form-item label="设备位置">
          <el-input v-model="registerForm.location" placeholder="请输入设备位置" />
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
    <!-- 修改内容：修改人：pengjunlin 时间：2026-08-04 17:30:00 -- end ---- -->

    <!-- 修改内容：修改人：pengjunlin 时间：2026-08-04 17:30:00 -- start ---- -->
    <!-- P0-5: 设备详情对话框，调用 getDeviceById 展示完整信息 -->
    <el-dialog v-model="detailDialogVisible" title="设备详情" width="640px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="设备ID">{{ detailForm.deviceId }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ detailForm.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">{{ detailForm.deviceType }}</el-descriptions-item>
        <el-descriptions-item label="设备型号">{{ detailForm.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="固件版本">{{ detailForm.firmwareVersion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(detailForm.status)">
            {{ getStatusText(detailForm.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="设备位置">{{ detailForm.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="心跳时间">{{ detailForm.lastHeartbeat || '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detailForm.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ detailForm.createTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
    <!-- 修改内容：修改人：pengjunlin 时间：2026-08-04 17:30:00 -- end ---- -->

    <!-- 修改内容：修改人：pengjunlin 时间：2026-08-04 17:30:00 -- start ---- -->
    <!-- P0-5: 编辑设备状态对话框，调用 updateDeviceStatus -->
    <el-dialog v-model="editDialogVisible" title="编辑设备状态" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="设备名称">
          <el-input v-model="editForm.deviceName" disabled />
        </el-form-item>
        <el-form-item label="设备状态" required>
          <el-select v-model="editForm.status" placeholder="请选择设备状态">
            <el-option label="在线" value="online" />
            <el-option label="离线" value="offline" />
            <el-option label="未激活" value="inactive" />
            <el-option label="禁用" value="disabled" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
    <!-- 修改内容：修改人：pengjunlin 时间：2026-08-04 17:30:00 -- end ---- -->
  </div>
</template>

<script setup lang="ts">
// 修改内容：修改人：pengjunlin 时间：2026-08-04 17:30:00 -- start ----
// P0-6: loadDevices 传入搜索参数并改用 pageNum/pageSize
// P0-5: handleView/handleEdit 改为打开详情/编辑对话框
// 注册表单补充 deviceId、location 字段
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeviceList, registerDevice, deleteDevice, getDeviceById, updateDeviceStatus } from '@/api/device'
import type { Device, DeviceRegisterRequest, DeviceQueryParams } from '@/api/device'

const devices = ref<Device[]>([])
const dialogVisible = ref(false)
const detailDialogVisible = ref(false)
const editDialogVisible = ref(false)

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
  deviceId: '',
  deviceName: '',
  deviceType: '',
  model: '',
  location: '',
  description: '',
})

// 详情表单
const detailForm = ref<Device>({} as Device)

// 编辑表单
const editForm = reactive({
  id: 0,
  deviceName: '',
  status: '',
})

onMounted(() => {
  loadDevices()
})

// P0-6: 调用 /devices/query，传入 pageNum/pageSize 及搜索条件
const loadDevices = async () => {
  try {
    const params: DeviceQueryParams = {
      pageNum: pagination.page,
      pageSize: pagination.size,
      deviceName: searchForm.deviceName,
      status: searchForm.status,
    }
    const data = await getDeviceList(params)
    devices.value = data.records
    pagination.total = data.total
  } catch (error) {
    console.error('Load devices error:', error)
  }
}

// P0-6: 搜索时重置页码为 1
const handleSearch = () => {
  pagination.page = 1
  loadDevices()
}

const handleRegister = () => {
  dialogVisible.value = true
  Object.assign(registerForm, {
    deviceId: '',
    deviceName: '',
    deviceType: '',
    model: '',
    location: '',
    description: '',
  })
}

const submitRegister = async () => {
  if (!registerForm.deviceId || !registerForm.deviceName || !registerForm.deviceType) {
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

// P0-5: 打开详情对话框，调用 getDeviceById 获取完整信息
const handleView = async (row: Device) => {
  try {
    const data = await getDeviceById(row.id)
    detailForm.value = data
    detailDialogVisible.value = true
  } catch (error) {
    console.error('Get device error:', error)
  }
}

// P0-5: 打开编辑对话框，允许修改设备状态
const handleEdit = (row: Device) => {
  editForm.id = row.id
  editForm.deviceName = row.deviceName
  editForm.status = row.status
  editDialogVisible.value = true
}

// P0-5: 保存编辑，调用 updateDeviceStatus
const submitEdit = async () => {
  if (!editForm.status) {
    ElMessage.warning('请选择设备状态')
    return
  }
  try {
    await updateDeviceStatus(editForm.id, editForm.status)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    loadDevices()
  } catch (error) {
    console.error('Update error:', error)
  }
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
// 修改内容：修改人：pengjunlin 时间：2026-08-04 17:30:00 -- end ----
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
