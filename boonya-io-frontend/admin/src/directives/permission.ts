import type { Directive } from 'vue'
import { useUserStore } from '@/stores/user'

export const permission: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const userStore = useUserStore()

    // admin 拥有所有权限
    if (userStore.isAdmin) return

    const requiredPermissions = Array.isArray(binding.value) ? binding.value : [binding.value]
    const hasPermission = requiredPermissions.some(p => userStore.hasPermission(p))

    if (!hasPermission) {
      el.parentNode?.removeChild(el)
    }
  }
}
