import request from '@/utils/request'

// 修改内容：修改人：pengjunlin 时间：2026-08-04 17:25:00 -- start ----
// 新增 OTA 任务管理 API，对接后端 OTA 服务

// OTA 任务
export interface OtaTask {
  id: number
  deviceId: string
  firmwareId: number
  status: string
  errorMessage?: string
  progress: number
  startTime?: string
  completeTime?: string
  createTime?: string
  updateTime?: string
}

/**
 * 创建 OTA 任务
 */
export function createOtaTask(deviceId: string, firmwareId: number) {
  return request.post<OtaTask>('/ota/tasks', null, {
    params: { deviceId, firmwareId },
  })
}

/**
 * 获取任务详情
 */
export function getOtaTaskById(id: number) {
  return request.get<OtaTask>(`/ota/tasks/${id}`)
}

/**
 * 获取设备任务列表
 */
export function getDeviceOtaTasks(deviceId: string) {
  return request.get<OtaTask[]>(`/ota/tasks/device/${deviceId}`)
}

/**
 * 更新任务状态（设备端上报）
 */
export function updateOtaTaskStatus(
  id: number,
  data: { status: string; progress?: number; errorMessage?: string }
) {
  return request.put<OtaTask>(`/ota/tasks/${id}/status`, data)
}

/**
 * 取消任务
 */
export function cancelOtaTask(id: number) {
  return request.post<OtaTask>(`/ota/tasks/${id}/cancel`)
}
// 修改内容：修改人：pengjunlin 时间：2026-08-04 17:25:00 -- end ----
