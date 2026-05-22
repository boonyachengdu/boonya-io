import request from '@/utils/request'

// 设备信息
export interface Device {
  id: number
  deviceId: string
  deviceName: string
  deviceType: string
  status: string
  authToken: string
  description: string
  createTime: string
  updateTime: string
}

// 设备注册请求
export interface DeviceRegisterRequest {
  deviceName: string
  deviceType: string
  protocol?: string
  description?: string
}

/**
 * 获取设备列表
 */
export function getDeviceList(params?: { page?: number; size?: number }) {
  return request.get<{ records: Device[]; total: number }>('/devices', { params })
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
export function activateDevice(deviceId: string, authToken: string) {
  return request.post(`/devices/${deviceId}/activate`, null, { params: { authToken } })
}

/**
 * 删除设备
 */
export function deleteDevice(id: number) {
  return request.delete(`/devices/${id}`)
}

/**
 * 更新设备
 */
export function updateDevice(id: number, data: Partial<Device>) {
  return request.put<Device>(`/devices/${id}`, data)
}
