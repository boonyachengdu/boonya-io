// 实时推送相关类型（MQTT 主题 payload）

/** 告警（主题 alerts/{deviceId}） */
export interface RealtimeAlert {
  message: string
  deviceId: string
  temp: number
  timestamp: number
}

/** 设备遥测（主题 device/{deviceId}/telemetry） */
export interface RealtimeTelemetry {
  temp: number
  ts?: number
  timestamp?: number
  [key: string]: any
}

/** MQTT 主题约定 */
export const TOPIC_ALERTS = 'alerts/#'
export const topicDeviceTelemetry = (deviceId: string) => `device/${deviceId}/telemetry`
