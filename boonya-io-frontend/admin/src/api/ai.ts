import request from '@/utils/request'

// ===== 类型定义 =====

export interface DiagnosisStatistics {
  count: number
  mean: number
  std: number
  max: number
  min: number
  range: number
}

export interface DeviceDiagnosis {
  deviceId: string
  timestamp: number
  status: string
  message?: string
  dataPoints: number
  statistics?: DiagnosisStatistics
  anomalies: string[]
  anomalyCount: number
  suggestions: string[]
}

export interface PredictedValue {
  value: number
  lowerBound: number
  upperBound: number
}

export interface TrendPrediction {
  deviceId: string
  predictMinutes: number
  timestamp: number
  status: string
  message?: string
  predicted?: PredictedValue
  trend: string
  slope: number
  currentValue: number
  risks: string[]
}

// ===== API =====

export function diagnoseDevice(deviceId: string) {
  return request.get<DeviceDiagnosis>(`/iot/ai/device/${deviceId}/diagnosis`)
}

export function predictTrend(deviceId: string, minutes: number = 60) {
  return request.post<TrendPrediction>(`/iot/ai/device/${deviceId}/predict`, null, {
    params: { minutes },
  })
}
