<template>
  <div class="role-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新增角色
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="角色名称">
          <el-input v-model="searchForm.roleName" placeholder="请输入角色名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 角色表格 -->
      <el-table :data="roles" border stripe v-loading="loading">
        <el-table-column prop="roleName" label="角色名称" />
        <el-table-column prop="roleCode" label="角色编码" width="180" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="success" @click="handlePermission(row)">分配权限</el-button>
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
        @size-change="loadRoles"
        @current-change="loadRoles"
      />
    </el-card>

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑角色' : '新增角色'" width="520px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="请输入角色编码" :disabled="!!editId" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入角色描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限弹窗 -->
    <el-dialog v-model="permissionDialogVisible" title="分配权限" width="560px">
      <el-form label-width="80px">
        <el-form-item label="角色名称">
          <el-input :model-value="currentRole.roleName" disabled />
        </el-form-item>
        <el-form-item label="权限">
          <div class="tree-wrapper" v-loading="permissionLoading">
            <el-tree
              ref="treeRef"
              :data="permissionTree"
              node-key="id"
              show-checkbox
              :props="{ label: 'name', children: 'children' }"
              :default-expand-all="true"
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPermission">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  getRoleList,
  createRole,
  updateRole,
  deleteRole,
  getPermissionTree,
  getRolePermissions,
  assignRolePermissions,
} from '@/api/user'
import type { RoleResponse, RoleCreateRequest, PermissionItem } from '@/api/user'

const loading = ref(false)
const roles = ref<RoleResponse[]>([])

const dialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const permissionLoading = ref(false)

const formRef = ref<FormInstance>()
const treeRef = ref()

const searchForm = reactive({ roleName: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

const editId = ref<number | null>(null)
const form = reactive<RoleCreateRequest>({ roleName: '', roleCode: '', description: '' })
const currentRole = ref<RoleResponse>({} as RoleResponse)
const permissionTree = ref<PermissionItem[]>([])

const formRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
}

onMounted(() => {
  loadRoles()
})

// 加载角色列表
const loadRoles = async () => {
  loading.value = true
  try {
    const data = await getRoleList({
      page: pagination.page,
      size: pagination.size,
      roleName: searchForm.roleName || undefined,
    })
    roles.value = data?.records || []
    pagination.total = data?.total ?? 0
  } catch (error) {
    console.error('Load roles error:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadRoles()
}

const handleReset = () => {
  searchForm.roleName = ''
  pagination.page = 1
  loadRoles()
}

const handleCreate = () => {
  editId.value = null
  Object.assign(form, { roleName: '', roleCode: '', description: '' })
  dialogVisible.value = true
}

const handleEdit = (row: RoleResponse) => {
  editId.value = row.id
  Object.assign(form, {
    roleName: row.roleName,
    roleCode: row.roleCode,
    description: row.description || '',
  })
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (editId.value) {
        await updateRole(editId.value, form)
        ElMessage.success('更新成功')
      } else {
        await createRole(form)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadRoles()
    } catch (error) {
      console.error('Submit role error:', error)
    }
  })
}

// 分配权限
const handlePermission = async (row: RoleResponse) => {
  currentRole.value = row
  permissionDialogVisible.value = true
  permissionLoading.value = true
  try {
    // 加载权限树
    if (!permissionTree.value.length) {
      permissionTree.value = (await getPermissionTree()) || []
    }
    // 回显已分配权限
    const ids = (await getRolePermissions(row.id)) || []
    // 等待树渲染后再设置选中
    setTimeout(() => {
      treeRef.value?.setCheckedKeys(ids)
    }, 0)
  } catch (error) {
    console.error('Load permission error:', error)
  } finally {
    permissionLoading.value = false
  }
}

const submitPermission = async () => {
  try {
    const checked = treeRef.value?.getCheckedKeys() || []
    const halfChecked = treeRef.value?.getHalfCheckedKeys() || []
    const permissionIds = [...checked, ...halfChecked]
    await assignRolePermissions(currentRole.value.id, permissionIds)
    ElMessage.success('权限分配成功')
    permissionDialogVisible.value = false
  } catch (error) {
    console.error('Assign permission error:', error)
  }
}

const handleDelete = async (row: RoleResponse) => {
  try {
    await ElMessageBox.confirm(`确定要删除角色「${row.roleName}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    loadRoles()
  } catch (error) {
    console.error('Delete role error:', error)
  }
}
</script>

<style scoped>
.role-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tree-wrapper {
  width: 100%;
  max-height: 360px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px;
}
</style>
