import request from '@/utils/request'

// 修正设备列表端点为 /devices/query，参数改为 pageNum/pageSize，支持搜索条件

// 设备信息
export interface Device {
  id: number
  deviceId: string
  deviceName: string
  deviceType: string
  model?: string
  firmwareVersion?: string
  status: string
  lastHeartbeat?: string
  groupId?: number
  location?: string
  description?: string
  authToken?: string
  createTime: string
  updateTime?: string
}

// 设备注册请求
export interface DeviceRegisterRequest {
  deviceId: string
  deviceName: string
  deviceType?: string
  model?: string
  location?: string
  description?: string
}

// 设备查询参数
export interface DeviceQueryParams {
  pageNum?: number
  pageSize?: number
  deviceId?: string
  deviceName?: string
  deviceType?: string
  status?: string
  groupId?: number
}

// 分页结果
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages?: number
}

/**
 * 获取设备列表（分页 + 搜索）
 */
export function getDeviceList(params?: DeviceQueryParams) {
  return request.get<PageResult<Device>>('/devices/query', { params })
}

/**
 * 获取设备详情
 */
export function getDeviceById(id: number) {
  return request.get<Device>(`/devices/${id}`)
}

/**
 * 注册设备
 */
export function registerDevice(data: DeviceRegisterRequest) {
  return request.post<Device>('/devices/register', data)
}

/**
 * 激活设备
 */
export function activateDevice(deviceId: string) {
  return request.post(`/devices/${deviceId}/activate`)
}

/**
 * 删除设备
 */
export function deleteDevice(id: number) {
  return request.delete(`/devices/${id}`)
}

/**
 * 更新设备状态
 */
export function updateDeviceStatus(id: number, status: string) {
  return request.put(`/devices/${id}`, null, { params: { status } })
}

/**
 * 获取在线设备列表
 */
export function getOnlineDevices() {
  return request.get<Device[]>('/devices/online')
}

/**
 * 获取设备状态
 */
export function getDeviceStatus(deviceId: string) {
  return request.get<{ deviceId: string; status: string }>(`/devices/${deviceId}/status`)
}
