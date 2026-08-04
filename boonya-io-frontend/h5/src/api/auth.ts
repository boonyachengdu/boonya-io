// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:20:00 -- start ----
import request from '@/utils/request'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo: {
    id: number
    username: string
    realName: string
    email: string
    roles: string[]
  }
}

export function login(data: LoginRequest) {
  return request.post<LoginResponse>('/auth/login', data)
}

export function logout() {
  return request.post('/auth/logout')
}

export function refreshToken(refreshToken: string) {
  return request.post<LoginResponse>('/auth/refresh', { refreshToken })
}
// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:20:00 -- end ----
