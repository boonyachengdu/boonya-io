import request from '@/utils/request'

// 登录请求
export interface LoginRequest {
  username: string
  password: string
}

// 登录响应
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

/**
 * 用户登录
 */
export function login(data: LoginRequest) {
  return request.post<LoginResponse>('/auth/login', data)
}

/**
 * 用户登出
 */
export function logout() {
  return request.post('/auth/logout')
}

/**
 * 刷新 Token
 */
export function refreshToken(refreshToken: string) {
  return request.post<LoginResponse>('/auth/refresh', { refreshToken })
}
