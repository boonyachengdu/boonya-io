import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi } from '@/api/auth'
import type { LoginRequest, LoginResponse } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<any>(null)

  const isLoggedIn = computed(() => !!token.value)

  /**
   * 登录
   */
  async function login(loginData: LoginRequest) {
    try {
      const data: LoginResponse = await loginApi(loginData)
      token.value = data.accessToken
      userInfo.value = data.userInfo
      localStorage.setItem('token', data.accessToken)
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
    } finally {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
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
