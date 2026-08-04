<template>
  <div class="firmware-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>固件管理</span>
          <el-button type="primary" @click="handleUpload">
            <el-icon><Upload /></el-icon>
            上传固件
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="设备型号">
          <el-input v-model="searchForm.deviceModel" placeholder="请输入设备型号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
            <el-option label="已归档" value="archived" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 固件列表 -->
      <el-table :data="firmwares" border stripe v-loading="loading">
        <el-table-column prop="deviceModel" label="设备型号" width="140" />
        <el-table-column prop="version" label="版本号" width="120" />
        <el-table-column prop="description" label="更新说明" show-overflow-tooltip />
        <el-table-column prop="fileName" label="文件名" show-overflow-tooltip />
        <el-table-column prop="fileSize" label="文件大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="forceUpdate" label="强制升级" width="100">
          <template #default="{ row }">
            <el-tag :type="row.forceUpdate ? 'danger' : 'info'" size="small">
              {{ row.forceUpdate ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column prop="publishTime" label="发布时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="row.status === 'draft'"
              link
              type="success"
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button
              v-if="row.status === 'published'"
              link
              type="warning"
              @click="handleArchive(row)"
            >
              归档
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
              link
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 上传固件对话框 -->
    <el-dialog v-model="dialogVisible" title="上传固件" width="560px">
      <el-form :model="uploadForm" label-width="100px">
        <el-form-item label="设备型号" required>
          <el-input v-model="uploadForm.deviceModel" placeholder="请输入设备型号" />
        </el-form-item>
        <el-form-item label="版本号" required>
          <el-input v-model="uploadForm.version" placeholder="请输入版本号，如 1.0.0" />
        </el-form-item>
        <el-form-item label="更新说明">
          <el-input
            v-model="uploadForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入更新说明"
          />
        </el-form-item>
        <el-form-item label="强制升级">
          <el-switch v-model="uploadForm.forceUpdate" />
        </el-form-item>
        <el-form-item label="固件文件" required>
          <el-upload
            :auto-upload="false"
            :limit="1"
            :on-exceed="handleExceed"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-button type="primary">
              <el-icon><Upload /></el-icon>
              选择文件
            </el-button>
            <template #tip>
              <div class="el-upload__tip">请选择固件文件，且只能上传一个文件</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile } from 'element-plus'
import {
  getFirmwareList,
  uploadFirmware,
  publishFirmware,
  archiveFirmware,
  deleteFirmware,
} from '@/api/firmware'
import type { Firmware } from '@/api/firmware'

// 修改内容：修改人：pengjunlin 时间：2026-08-04 17:40:00 -- start ----
// 重写固件管理页面，对接后端固件管理 API：列表查询/筛选、上传、发布、归档、删除、查看
const firmwares = ref<Firmware[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const uploading = ref(false)

const searchForm = reactive({
  deviceModel: '',
  status: '',
})

const uploadForm = reactive({
  deviceModel: '',
  version: '',
  description: '',
  forceUpdate: false,
  file: null as File | null,
})

onMounted(() => {
  loadFirmwares()
})

const loadFirmwares = async () => {
  loading.value = true
  try {
    const params: { deviceModel?: string; status?: string } = {}
    if (searchForm.deviceModel) params.deviceModel = searchForm.deviceModel
    if (searchForm.status) params.status = searchForm.status
    const data = await getFirmwareList(params)
    firmwares.value = data || []
  } catch (error) {
    console.error('Load firmwares error:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  loadFirmwares()
}

const handleReset = () => {
  searchForm.deviceModel = ''
  searchForm.status = ''
  loadFirmwares()
}

const handleUpload = () => {
  Object.assign(uploadForm, {
    deviceModel: '',
    version: '',
    description: '',
    forceUpdate: false,
    file: null,
  })
  dialogVisible.value = true
}

const handleFileChange = (file: UploadFile) => {
  uploadForm.file = file.raw
}

const handleFileRemove = () => {
  uploadForm.file = null
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件，请先移除已选文件')
}

const submitUpload = async () => {
  if (!uploadForm.deviceModel) {
    ElMessage.warning('请输入设备型号')
    return
  }
  if (!uploadForm.version) {
    ElMessage.warning('请输入版本号')
    return
  }
  if (!uploadForm.file) {
    ElMessage.warning('请选择固件文件')
    return
  }

  uploading.value = true
  try {
    await uploadFirmware({
      deviceModel: uploadForm.deviceModel,
      version: uploadForm.version,
      description: uploadForm.description,
      forceUpdate: uploadForm.forceUpdate,
      file: uploadForm.file,
    })
    ElMessage.success('上传成功')
    dialogVisible.value = false
    loadFirmwares()
  } catch (error) {
    console.error('Upload firmware error:', error)
  } finally {
    uploading.value = false
  }
}

const handleView = (row: Firmware) => {
  const lines = [
    `设备型号：${row.deviceModel}`,
    `版本号：${row.version}`,
    `更新说明：${row.description || '-'}`,
    `文件名：${row.fileName || '-'}`,
    `文件大小：${formatFileSize(row.fileSize)}`,
    `MD5校验：${row.md5Checksum || '-'}`,
    `状态：${getStatusText(row.status)}`,
    `强制升级：${row.forceUpdate ? '是' : '否'}`,
    `创建时间：${row.createTime || '-'}`,
    `发布时间：${row.publishTime || '-'}`,
  ]
  ElMessageBox.alert(lines.join('<br>'), '固件详情', {
    confirmButtonText: '确定',
    dangerouslyUseHTMLString: true,
  })
}

const handlePublish = async (row: Firmware) => {
  await ElMessageBox.confirm(
    `确定要发布固件 ${row.deviceModel} v${row.version} 吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
  try {
    await publishFirmware(row.id)
    ElMessage.success('发布成功')
    loadFirmwares()
  } catch (error) {
    console.error('Publish firmware error:', error)
  }
}

const handleArchive = async (row: Firmware) => {
  await ElMessageBox.confirm(
    `确定要归档固件 ${row.deviceModel} v${row.version} 吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
  try {
    await archiveFirmware(row.id)
    ElMessage.success('归档成功')
    loadFirmwares()
  } catch (error) {
    console.error('Archive firmware error:', error)
  }
}

const handleDelete = async (row: Firmware) => {
  await ElMessageBox.confirm(
    `确定要删除固件 ${row.deviceModel} v${row.version} 吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
  try {
    await deleteFirmware(row.id)
    ElMessage.success('删除成功')
    loadFirmwares()
  } catch (error) {
    console.error('Delete firmware error:', error)
  }
}

const formatFileSize = (bytes?: number) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

const getStatusType = (status: string) => {
  const types: Record<string, any> = {
    draft: 'info',
    published: 'success',
    archived: 'warning',
  }
  return types[status] || 'info'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    draft: '草稿',
    published: '已发布',
    archived: '已归档',
  }
  return texts[status] || status
}
// 修改内容：修改人：pengjunlin 时间：2026-08-04 17:40:00 -- end ----
</script>

<style scoped>
.firmware-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
