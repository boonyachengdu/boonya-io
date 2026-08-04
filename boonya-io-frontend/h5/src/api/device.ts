// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:20:00 -- start ----
import request from '@/utils/request'

export interface Device {
  id: number
  deviceId: string
  deviceName: string
  deviceType?: string
  model?: string
  firmwareVersion?: string
  status: string
  lastHeartbeat?: string
  location?: string
  description?: string
  createTime: string
}

export interface DeviceQueryParams {
  pageNum?: number
  pageSize?: number
  deviceName?: string
  status?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export function getDeviceList(params?: DeviceQueryParams) {
  return request.get<PageResult<Device>>('/devices/query', { params })
}

export function getDeviceById(id: number) {
  return request.get<Device>(`/devices/${id}`)
}

export function registerDevice(data: Partial<Device>) {
  return request.post<Device>('/devices/register', data)
}
// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:20:00 -- end ----
