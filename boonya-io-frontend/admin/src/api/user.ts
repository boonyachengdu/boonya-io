import request from '@/utils/request'

// ===== 类型定义 =====

export interface RoleResponse {
  id: number
  roleName: string
  roleCode: string
  description?: string
  createTime?: string
}

export interface UserResponse {
  id: number
  username: string
  email?: string
  phone?: string
  realName?: string
  status: string
  lastLoginTime?: string
  createTime?: string
  roles: RoleResponse[]
}

export interface UserCreateRequest {
  username: string
  password: string
  email?: string
  phone?: string
  realName?: string
  roleIds?: number[]
}

export interface UserQueryParams {
  page?: number
  size?: number
  username?: string
  realName?: string
  status?: string
}

export interface RoleCreateRequest {
  roleName: string
  roleCode: string
  description?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

// ===== 用户管理 API =====

export function getUserList(params?: UserQueryParams) {
  return request.get<PageResult<UserResponse>>('/users/query', { params })
}

export function getUserById(id: number) {
  return request.get<UserResponse>(`/users/${id}`)
}

export function createUser(data: UserCreateRequest) {
  return request.post<UserResponse>('/users', data)
}

export function updateUserStatus(id: number, status: string) {
  return request.put(`/users/${id}/status`, null, { params: { status } })
}

export function resetPassword(id: number, newPassword: string) {
  return request.put(`/users/${id}/password`, { newPassword })
}

export function assignRoles(id: number, roleIds: number[]) {
  return request.put(`/users/${id}/roles`, { roleIds })
}

export function deleteUser(id: number) {
  return request.delete(`/users/${id}`)
}

// ===== 角色管理 API =====

export function getAllRoles() {
  return request.get<RoleResponse[]>('/roles')
}

export function getRoleList(params?: { page?: number; size?: number; roleName?: string }) {
  return request.get<PageResult<RoleResponse>>('/roles/query', { params })
}

export function createRole(data: RoleCreateRequest) {
  return request.post<RoleResponse>('/roles', data)
}

export function updateRole(id: number, data: RoleCreateRequest) {
  return request.put<RoleResponse>(`/roles/${id}`, data)
}

export function deleteRole(id: number) {
  return request.delete(`/roles/${id}`)
}
