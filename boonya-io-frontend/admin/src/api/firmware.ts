import request from '@/utils/request'
import type { PageResult } from './device'

// 新增固件管理 API，对接后端 OTA 服务

// 固件信息
export interface Firmware {
  id: number
  deviceModel: string
  version: string
  description?: string
  filePath?: string
  fileName?: string
  fileSize?: number
  md5Checksum?: string
  status: string
  forceUpdate?: boolean
  createTime?: string
  updateTime?: string
  publishTime?: string
}

// 固件分页查询参数
export interface FirmwareQueryParams {
  pageNum?: number
  pageSize?: number
  deviceModel?: string
  status?: string
}

/**
 * 获取固件列表（分页）
 */
export function getFirmwareList(params?: FirmwareQueryParams) {
  return request.get<PageResult<Firmware>>('/firmware', { params })
}

/**
 * 获取固件详情
 */
export function getFirmwareById(id: number) {
  return request.get<Firmware>(`/firmware/${id}`)
}

/**
 * 上传固件（multipart 表单）
 */
export function uploadFirmware(data: {
  deviceModel: string
  version: string
  description?: string
  forceUpdate?: boolean
  file: File
}) {
  const formData = new FormData()
  formData.append('deviceModel', data.deviceModel)
  formData.append('version', data.version)
  if (data.description) formData.append('description', data.description)
  formData.append('forceUpdate', String(data.forceUpdate ?? false))
  formData.append('file', data.file)

  return request.post<Firmware>('/firmware', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/**
 * 发布固件
 */
export function publishFirmware(id: number) {
  return request.post<Firmware>(`/firmware/${id}/publish`)
}

/**
 * 归档固件
 */
export function archiveFirmware(id: number) {
  return request.post<Firmware>(`/firmware/${id}/archive`)
}

/**
 * 删除固件（仅草稿状态）
 */
export function deleteFirmware(id: number) {
  return request.delete(`/firmware/${id}`)
}
