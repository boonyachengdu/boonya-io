// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:20:00 -- start ----
import request from '@/utils/request'

export interface DeviceRealtimeData {
  deviceId: string
  latestTemp?: number
  latestTimestamp?: number
  todayAvgTemp?: number
  todayMaxTemp?: number
  todayMinTemp?: number
  dataPoints?: number
}

export interface TrendPoint {
  timestamp: number
  value: number
}

export function getDeviceRealtime(deviceId: string) {
  return request.get<DeviceRealtimeData>(`/analytics/device/${deviceId}/realtime`)
}

export function getDeviceTrend(deviceId: string, period: string = '24h') {
  return request.get<TrendPoint[]>(`/analytics/device/${deviceId}/trend`, { params: { period } })
}
// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:20:00 -- end ----
