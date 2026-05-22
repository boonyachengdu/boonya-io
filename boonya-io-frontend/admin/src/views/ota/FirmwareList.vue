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
      
      <el-table :data="firmwares" border stripe>
        <el-table-column prop="deviceModel" label="设备型号" />
        <el-table-column prop="version" label="版本号" />
        <el-table-column prop="fileName" label="文件名" />
        <el-table-column prop="fileSize" label="文件大小">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'draft'"
              link
              type="primary"
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button link type="primary" @click="handleDownload(row)">
              下载
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const firmwares = ref([])

const formatFileSize = (bytes: number) => {
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

const handleUpload = () => {
  ElMessage.info('上传固件功能开发中')
}

const handlePublish = (row: any) => {
  ElMessage.info('发布固件功能开发中')
}

const handleDownload = (row: any) => {
  ElMessage.info('下载固件功能开发中')
}

const handleDelete = (row: any) => {
  ElMessage.info('删除固件功能开发中')
}
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
