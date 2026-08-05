import request from '@/utils/request'

export interface EnergyOverview {
  siteName: string
  period: string
  electricityKwh: number
  waterM3: number
  solarKwh: number
  storageDischargeKwh: number
  energyCostCny: number
  carbonTons: number
  carbonReductionTons: number
  activeAlarms: number
  onlineDevices: number
  totalDevices: number
}

export interface EnergyTrendPoint {
  time: string
  electricityKwh: number
  waterM3: number
  solarKwh: number
  carbonTons: number
}

export interface AreaRankingItem {
  name: string
  electricityKwh: number
  waterM3: number
  carbonTons: number
  trend: 'up' | 'down' | 'stable'
}

export interface EnergyDeviceStatus {
  deviceId: string
  deviceName: string
  deviceType: string
  status: 'online' | 'offline' | 'warning'
  value: number
  unit: string
  location: string
}

export interface EnergyAlarm {
  level: 'high' | 'medium' | 'low'
  title: string
  deviceId: string
  description: string
  status: string
  time: string
}

export function getEnergyOverview() {
  return request.get<EnergyOverview>('/analytics/energy/overview')
}

export function getEnergyTrend(period = 'day') {
  return request.get<EnergyTrendPoint[]>('/analytics/energy/trend', { params: { period } })
}

export function getAreaRanking() {
  return request.get<AreaRankingItem[]>('/analytics/energy/areas/ranking')
}

export function getEnergyDeviceStatus() {
  return request.get<EnergyDeviceStatus[]>('/analytics/energy/devices/status')
}

export function getEnergyAlarms() {
  return request.get<EnergyAlarm[]>('/analytics/energy/alarms')
}
