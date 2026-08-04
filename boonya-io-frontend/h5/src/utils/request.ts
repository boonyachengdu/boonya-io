import axios from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'
import { showToast } from 'vant'

const service = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

service.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:00:00 -- start ----
// Token 自动刷新 + 兼容两种后端响应格式

let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

async function doRefreshToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return null

  try {
    const response = await axios.post('/api/auth/refresh', { refreshToken })
    const data = response.data
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

function handle401(originalConfig: InternalAxiosRequestConfig): Promise<any> {
  if (isRefreshing) {
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
        pendingRequests.forEach((cb) => cb(newToken!))
        pendingRequests = []
        originalConfig.headers.Authorization = `Bearer ${newToken}`
        return service(originalConfig)
      } else {
        pendingRequests = []
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        showToast('登录已过期，请重新登录')
        window.location.href = '/login'
        return Promise.reject(new Error('Token refresh failed'))
      }
    })
    .finally(() => {
      isRefreshing = false
    })
}

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && typeof res.code === 'number') {
      if (res.code !== 200) {
        if (res.code === 401) {
          return handle401(response.config as InternalAxiosRequestConfig)
        }
        showToast(res.message || '请求失败')
        return Promise.reject(new Error(res.message))
      }
      return res.data
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      return handle401(error.config)
    }
    showToast(error.response?.data?.message || error.message || '网络错误')
    return Promise.reject(error)
  }
)
// 修改内容：修改人：pengjunlin 时间：2026-08-04 18:00:00 -- end ----

export default service
