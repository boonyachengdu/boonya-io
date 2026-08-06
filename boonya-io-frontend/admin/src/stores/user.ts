import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi } from '@/api/auth'
import type { LoginRequest, LoginResponse } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<any>(localStorage.getItem('userInfo') ? JSON.parse(localStorage.getItem('userInfo')!) : null)

  const isLoggedIn = computed(() => !!token.value)

  /**
   * 登录
   */
  // 登录时存储 refreshToken，登出时清除
  async function login(loginData: LoginRequest) {
    try {
      const data: LoginResponse = await loginApi(loginData)
      token.value = data.accessToken
      userInfo.value = data.userInfo
      localStorage.setItem('token', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      // 持久化 userInfo 以支持刷新页面后保留
      localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
      return data
    } catch (error) {
      throw error
    }
  }

  /**
   * 登出
   */
  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 后端 logout 失败不影响本地清理
    } finally {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userInfo')
    }
  }

  /**
   * 设置用户信息
   */
  function setUserInfo(info: any) {
    userInfo.value = info
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    logout,
    setUserInfo,
  }
})
