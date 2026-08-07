<template>
  <div class="user-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新增用户
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.realName" placeholder="请输入真实姓名" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 120px">
            <el-option label="正常" value="active" />
            <el-option label="未激活" value="inactive" />
            <el-option label="锁定" value="locked" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 用户表格 -->
      <el-table :data="users" border stripe>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="email" label="邮箱" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="角色" width="160">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role.id" size="small" style="margin-right: 4px">
              {{ role.roleName }}
            </el-tag>
            <span v-if="!row.roles?.length">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="170" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="330" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="handleAssignRoles(row)">分配角色</el-button>
            <el-button link type="warning" @click="handleResetPassword(row)">重置密码</el-button>
            <el-button link type="danger" @click="handleToggleStatus(row)">
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @size-change="loadUsers"
        @current-change="loadUsers"
      />
    </el-card>

    <!-- 新增用户对话框 -->
    <el-dialog v-model="createDialogVisible" title="新增用户" width="560px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="createForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="createForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in allRoles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑用户对话框（仅修改 realName/email/phone） -->
    <el-dialog v-model="editDialogVisible" title="编辑用户" width="520px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="用户名">
          <el-input :model-value="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="editForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色对话框 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="480px">
      <el-form label-width="80px">
        <el-form-item label="用户名">
          <el-input :model-value="currentUser.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="selectedRoleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in allRoles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssignRoles">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="passwordDialogVisible" title="重置密码" width="440px">
      <el-form label-width="80px">
        <el-form-item label="用户名">
          <el-input :model-value="currentUser.username" disabled />
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="newPassword" type="password" placeholder="请输入新密码（至少6位）" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  getUserList,
  createUser,
  updateUser,
  updateUserStatus,
  resetPassword,
  assignRoles,
  deleteUser,
  getAllRoles,
} from '@/api/user'
import type { UserResponse, UserCreateRequest, UserUpdateRequest, RoleResponse } from '@/api/user'

const users = ref<UserResponse[]>([])
const allRoles = ref<RoleResponse[]>([])

const createDialogVisible = ref(false)
const roleDialogVisible = ref(false)
const passwordDialogVisible = ref(false)

const createFormRef = ref<FormInstance>()
const currentUser = ref<UserResponse>({} as UserResponse)
const selectedRoleIds = ref<number[]>([])
const newPassword = ref('')

const searchForm = reactive({ username: '', realName: '', status: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

const createForm = reactive<UserCreateRequest>({
  username: '',
  password: '',
  email: '',
  phone: '',
  realName: '',
  roleIds: [],
})

const createRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 64, message: '长度在3-64个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
}

// 编辑用户（仅修改 realName/email/phone）
const editDialogVisible = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive<{ id: number; username: string; realName: string; email: string; phone: string }>({
  id: 0,
  username: '',
  realName: '',
  email: '',
  phone: '',
})
const editRules = {
  realName: [{ max: 64, message: '长度不能超过64个字符', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
}

onMounted(() => {
  loadUsers()
  loadRoles()
})

const loadUsers = async () => {
  try {
    const data = await getUserList({
      page: pagination.page,
      size: pagination.size,
      username: searchForm.username || undefined,
      realName: searchForm.realName || undefined,
      status: searchForm.status || undefined,
    })
    users.value = data.records
    pagination.total = data.total
  } catch (error) {
    console.error('Load users error:', error)
  }
}

const loadRoles = async () => {
  try {
    allRoles.value = await getAllRoles()
  } catch (error) {
    console.error('Load roles error:', error)
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadUsers()
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.realName = ''
  searchForm.status = ''
  pagination.page = 1
  loadUsers()
}

const handleCreate = () => {
  Object.assign(createForm, { username: '', password: '', email: '', phone: '', realName: '', roleIds: [] })
  createDialogVisible.value = true
}

const submitCreate = async () => {
  if (!createFormRef.value) return
  await createFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await createUser(createForm)
      ElMessage.success('创建成功')
      createDialogVisible.value = false
      loadUsers()
    } catch (error) {
      console.error('Create user error:', error)
    }
  })
}

// 打开编辑弹窗（仅修改 realName/email/phone）
const handleEdit = (row: UserResponse) => {
  editForm.id = row.id
  editForm.username = row.username
  editForm.realName = row.realName || ''
  editForm.email = row.email || ''
  editForm.phone = row.phone || ''
  editDialogVisible.value = true
}

const submitEdit = async () => {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      const data: UserUpdateRequest = {
        realName: editForm.realName,
        email: editForm.email,
        phone: editForm.phone,
      }
      await updateUser(editForm.id, data)
      ElMessage.success('更新成功')
      editDialogVisible.value = false
      loadUsers()
    } catch (error) {
      console.error('Update user error:', error)
    }
  })
}

const handleAssignRoles = (row: UserResponse) => {
  currentUser.value = row
  selectedRoleIds.value = row.roles?.map((r) => r.id) || []
  roleDialogVisible.value = true
}

const submitAssignRoles = async () => {
  try {
    await assignRoles(currentUser.value.id, selectedRoleIds.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    loadUsers()
  } catch (error) {
    console.error('Assign roles error:', error)
  }
}

const handleResetPassword = (row: UserResponse) => {
  currentUser.value = row
  newPassword.value = ''
  passwordDialogVisible.value = true
}

const submitResetPassword = async () => {
  if (!newPassword.value || newPassword.value.length < 6) {
    ElMessage.warning('密码至少6位')
    return
  }
  try {
    await resetPassword(currentUser.value.id, newPassword.value)
    ElMessage.success('密码重置成功')
    passwordDialogVisible.value = false
  } catch (error) {
    console.error('Reset password error:', error)
  }
}

const handleToggleStatus = async (row: UserResponse) => {
  const newStatus = row.status === 'active' ? 'inactive' : 'active'
  const action = row.status === 'active' ? '禁用' : '启用'
  await ElMessageBox.confirm(`确定要${action}用户 ${row.username} 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  try {
    await updateUserStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    loadUsers()
  } catch (error) {
    console.error('Toggle status error:', error)
  }
}

const handleDelete = async (row: UserResponse) => {
  await ElMessageBox.confirm(`确定要删除用户 ${row.username} 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  try {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    loadUsers()
  } catch (error) {
    console.error('Delete user error:', error)
  }
}

const getStatusType = (status: string) => {
  const types: Record<string, string> = { active: 'success', inactive: 'info', locked: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = { active: '正常', inactive: '未激活', locked: '锁定' }
  return texts[status] || status
}
</script>

<style scoped>
.user-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
