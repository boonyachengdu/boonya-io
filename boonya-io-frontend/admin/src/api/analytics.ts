import request from '@/utils/request'

// 修改内容：修改人：pengjunlin 时间：2026-08-04 17:15:00 -- start ----
// 新增数据分析 API，对接后端 Analytics 服务

// 设备实时数据
export interface DeviceRealtimeData {
  deviceId: string
  latestTemp?: number
  latestTimestamp?: number
  todayAvgTemp?: number
  todayMaxTemp?: number
  todayMinTemp?: number
  dataPoints?: number
}

// 系统概览数据
export interface OverviewData {
  totalDevices: number
  onlineDevices: number
  todayDataPoints: number
}

// 趋势数据点
export interface TrendPoint {
  timestamp: number
  value: number
}

/**
 * 获取系统概览
 */
export function getOverview() {
  return request.get<OverviewData>('/analytics/overview')
}

/**
 * 获取设备实时数据
 */
export function getDeviceRealtime(deviceId: string) {
  return request.get<DeviceRealtimeData>(`/analytics/device/${deviceId}/realtime`)
}

/**
 * 获取设备趋势数据
 */
export function getDeviceTrend(deviceId: string, period: string = '24h') {
  return request.get<TrendPoint[]>(`/analytics/device/${deviceId}/trend`, {
    params: { period },
  })
}
// 修改内容：修改人：pengjunlin 时间：2026-08-04 17:15:00 -- end ----
