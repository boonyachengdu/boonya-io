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

// ===== 设备详情相关类型与 API =====

/** 设备历史数据点（用于趋势图） */
export interface DeviceHistoryPoint {
  timestamp: number
  temp?: number
  humidity?: number
  value?: number
  [key: string]: any
}

/** 操作日志记录 */
export interface DeviceLog {
  id: number
  deviceId: string
  action?: string
  operation?: string
  detail?: string
  operator?: string
  createTime?: string
  [key: string]: any
}

/** 设备日志查询参数 */
export interface DeviceLogQueryParams {
  pageNum?: number
  pageSize?: number
  deviceId?: string
  action?: string
}

/**
 * 根据 deviceId（字符串设备标识）获取设备详情
 */
export function getDeviceByDeviceId(deviceId: string) {
  return request.get<Device>(`/devices/by-device-id/${deviceId}`)
}

/**
 * 获取设备历史数据（温度趋势等）
 * @param deviceId 设备ID
 * @param timeRange 时间范围：1h / 6h / 24h
 */
export function getDeviceHistory(deviceId: string, timeRange: string = '24h') {
  return request.get<DeviceHistoryPoint[]>(`/devices/${deviceId}/history`, {
    params: { timeRange },
  })
}

/**
 * 获取设备操作日志（分页）
 */
export function getDeviceLogs(deviceId: string, params?: DeviceLogQueryParams) {
  return request.get<PageResult<DeviceLog>>(`/devices/${deviceId}/logs`, { params })
}

/**
 * 更新设备状态（按字符串 deviceId）
 */
export function updateDeviceStatusByDeviceId(deviceId: string, status: string) {
  return request.put(`/devices/by-device-id/${deviceId}`, null, { params: { status } })
}
