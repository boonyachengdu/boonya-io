import axios from 'axios'
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

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      showToast(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message))
    }
    return res.data
  },
  (error) => {
    showToast(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service
