import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi } from '@/api/auth'
import type { LoginRequest, LoginResponse } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<any>(localStorage.getItem('userInfo') ? JSON.parse(localStorage.getItem('userInfo')!) : null)
  const permissions = ref<string[]>(
    localStorage.getItem('permissions') ? JSON.parse(localStorage.getItem('permissions')!) : []
  )

  const isLoggedIn = computed(() => !!token.value)

  // 是否为管理员（roles 中包含 ROLE_ADMIN）
  const isAdmin = computed(() => {
    const roles: string[] = userInfo.value?.roles || []
    return roles.includes('ROLE_ADMIN')
  })

  /**
   * 登录
   */
  // 登录时存储 refreshToken，登出时清除
  async function login(loginData: LoginRequest) {
    try {
      const data: LoginResponse = await loginApi(loginData)
      token.value = data.accessToken
      userInfo.value = data.userInfo
      // 登录时存储 permissions（后端可能在 userInfo 中返回 permissions）
      const userPermissions: string[] = (data.userInfo as any)?.permissions || []
      permissions.value = userPermissions
      localStorage.setItem('token', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      // 持久化 userInfo 以支持刷新页面后保留
      localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
      localStorage.setItem('permissions', JSON.stringify(userPermissions))
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
      permissions.value = []
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('permissions')
    }
  }

  /**
   * 设置用户信息
   */
  function setUserInfo(info: any) {
    userInfo.value = info
  }

  /**
   * 判断当前用户是否拥有指定权限码
   */
  function hasPermission(code: string) {
    // 管理员拥有所有权限
    if (isAdmin.value) return true
    return permissions.value.includes(code)
  }

  return {
    token,
    userInfo,
    permissions,
    isLoggedIn,
    isAdmin,
    login,
    logout,
    setUserInfo,
    hasPermission,
  }
})
