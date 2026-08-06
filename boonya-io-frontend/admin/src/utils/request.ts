import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// Token 自动刷新 + 兼容两种后端响应格式

let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

/**
 * 用 RefreshToken 换取新的 AccessToken
 */
async function doRefreshToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return null

  try {
    const response = await axios.post('/api/auth/refresh', { refreshToken })
    const data = response.data
    // 兼容 Result<T> 包装
    const tokenData = data?.code === 200 ? data.data : data
    if (tokenData?.accessToken) {
      localStorage.setItem('token', tokenData.accessToken)
      if (tokenData.refreshToken) {
        localStorage.setItem('refreshToken', tokenData.refreshToken)
      }
      return tokenData.accessToken
    }
    return null
  } catch {
    return null
  }
}

/**
 * 处理 401：尝试刷新 Token 并重试原始请求
 */
function handle401(originalConfig: InternalAxiosRequestConfig): Promise<any> {
  if (isRefreshing) {
    // 已有刷新请求在进行中，排队等待
    return new Promise((resolve) => {
      pendingRequests.push((newToken: string) => {
        originalConfig.headers.Authorization = `Bearer ${newToken}`
        resolve(service(originalConfig))
      })
    })
  }

  isRefreshing = true
  return doRefreshToken()
    .then((newToken) => {
      if (newToken) {
        // 刷新成功，重试所有排队的请求
        pendingRequests.forEach((cb) => cb(newToken!))
        pendingRequests = []
        originalConfig.headers.Authorization = `Bearer ${newToken}`
        return service(originalConfig)
      } else {
        // 刷新失败，跳转登录
        pendingRequests = []
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        ElMessage.error('登录已过期，请重新登录')
        window.location.href = '/login'
        return Promise.reject(new Error('Token refresh failed'))
      }
    })
    .finally(() => {
      isRefreshing = false
    })
}

// 响应拦截器（兼容 Result<T> 和裸数据两种格式）
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data

    // 如果响应包含 code 字段，按 Result<T> 格式处理
    if (res && typeof res.code === 'number') {
      if (res.code !== 200) {
        if (res.code === 401) {
          return handle401(response.config as InternalAxiosRequestConfig)
        }
        ElMessage.error(res.message || '请求失败')
        return Promise.reject(new Error(res.message || '请求失败'))
      }
      return res.data
    }

    // 否则按裸数据返回（Device 等模块返回 ResponseEntity）
    return res
  },
  (error) => {
    console.error('Response error:', error)
    if (error.response?.status === 401) {
      return handle401(error.config)
    }
    ElMessage.error(error.response?.data?.message || error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service
