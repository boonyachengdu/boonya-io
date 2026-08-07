import request from '@/utils/request'

// ===== 告警相关类型 =====

/** 告警状态：PENDING 待处理 / ACKNOWLEDGED 已确认 / RESOLVED 已解决 / CLOSED 已关闭 */
export type AlertStatus = 'PENDING' | 'ACKNOWLEDGED' | 'RESOLVED' | 'CLOSED'

/** 告警严重级别：INFO 提示 / WARNING 警告 / CRITICAL 严重 */
export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL'

/** 历史告警记录 */
export interface AlertItem {
  id: number
  deviceId: string
  title?: string
  message?: string
  severity: AlertSeverity
  status: AlertStatus
  triggerTime: string
  acknowledgedTime?: string
  resolvedTime?: string
  closedTime?: string
  createTime?: string
}

/** 告警统计（用于历史告警顶部卡片） */
export interface AlertStatistics {
  todayTotal: number
  pending: number
  acknowledged: number
  resolved: number
}

/** 告警查询参数 */
export interface AlertQueryParams {
  pageNum?: number
  pageSize?: number
  deviceId?: string
  severity?: AlertSeverity
  status?: AlertStatus
  startTime?: string
  endTime?: string
}

/** 分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages?: number
}

// ===== 告警规则相关类型 =====

/** 运算符 */
export type AlertOperator = '>' | '<' | '>=' | '<=' | '==' | '!='

/** 告警规则 */
export interface AlertRule {
  id: number
  ruleName: string
  deviceId?: string
  metric: string
  operator: AlertOperator
  threshold: number
  severity: AlertSeverity
  cooldownMs?: number
  enabled: boolean
  createTime?: string
}

/** 新增规则请求 */
export interface AlertRuleCreateRequest {
  ruleName: string
  deviceId?: string
  metric: string
  operator: AlertOperator
  threshold: number
  severity: AlertSeverity
  cooldownMs?: number
}

/** 规则查询参数 */
export interface AlertRuleQueryParams {
  pageNum?: number
  pageSize?: number
  ruleName?: string
  deviceId?: string
}

// ===== 告警 API =====

/**
 * 获取历史告警列表（分页 + 筛选）
 */
export function getAlertList(params?: AlertQueryParams) {
  return request.get<PageResult<AlertItem>>('/alerts/query', { params })
}

/**
 * 获取告警统计（今日总数/待处理/已确认/已解决）
 */
export function getAlertStatistics(params?: { deviceId?: string }) {
  return request.get<AlertStatistics>('/alerts/statistics', { params })
}

/**
 * 确认告警（PENDING -> ACKNOWLEDGED）
 */
export function acknowledgeAlert(id: number) {
  return request.put(`/alerts/${id}/acknowledge`)
}

/**
 * 解决告警（ACKNOWLEDGED -> RESOLVED）
 */
export function resolveAlert(id: number) {
  return request.put(`/alerts/${id}/resolve`)
}

/**
 * 关闭告警（RESOLVED -> CLOSED）
 */
export function closeAlert(id: number) {
  return request.put(`/alerts/${id}/close`)
}

// ===== 告警规则 API =====

/**
 * 获取告警规则列表（分页）
 */
export function getAlertRules(params?: AlertRuleQueryParams) {
  return request.get<PageResult<AlertRule>>('/alerts/rules/query', { params })
}

/**
 * 新增告警规则
 */
export function createAlertRule(data: AlertRuleCreateRequest) {
  return request.post<AlertRule>('/alerts/rules', data)
}

/**
 * 启用告警规则
 */
export function enableAlertRule(id: number) {
  return request.put(`/alerts/rules/${id}/enable`)
}

/**
 * 禁用告警规则
 */
export function disableAlertRule(id: number) {
  return request.put(`/alerts/rules/${id}/disable`)
}

/**
 * 删除告警规则
 */
export function deleteAlertRule(id: number) {
  return request.delete(`/alerts/rules/${id}`)
}
