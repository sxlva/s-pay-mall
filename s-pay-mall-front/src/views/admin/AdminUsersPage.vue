<template>
  <div class="admin-content">
    <div class="page-header">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/admin' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>用户管理</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <el-card class="main-card" shadow="hover">
      <el-table :data="users" stripe border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="150">
          <template #default="scope">
            <div class="user-info">
              <el-avatar :icon="User" class="avatar" />
              <span>{{ scope.row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="role_code" label="角色" width="120">
          <template #default="scope">
            <el-tag :type="getRoleTagType(scope.row.role_code)">
              {{ getRoleText(scope.row.role_code) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="create_time" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button
              size="small"
              type="warning"
              link
              @click="handleToggleStatus(scope.row)"
            >
              {{ getToggleActionText(scope.row.status) }}
            </el-button>
            <el-button
              size="small"
              type="danger"
              link
              @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="users.length === 0" class="empty-state">
        <el-empty description="暂无用户数据" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import { AdminUserService } from '../../services/adminUserService'

interface UserVO {
  id: number
  username: string
  email: string
  role: string
  roleCode: string
  status: number
  createTime: string
  create_time: string
  updateTime: string
  update_time: string
}

const users = ref<UserVO[]>([])

const load = async () => {
  try {
    users.value = await AdminUserService.listUsers()
  } catch (error) {
    console.error('获取用户列表失败:', error)
  }
}

const handleToggleStatus = async (row: UserVO) => {
  const action = AdminUserService.getToggleActionText(row.status)
  try {
    await ElMessageBox.confirm(
      `确定要${action}该用户吗？`,
      '提示',
      { type: 'warning' }
    )

    await AdminUserService.toggleUserStatus(row.id, row.status)
    ElMessage.success(`${action}成功`)
    await load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

const handleDelete = async (row: UserVO) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该用户吗？此操作不可恢复！',
      '警告',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )

    await AdminUserService.removeUser(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const getStatusText = (status: number): string => {
  return AdminUserService.getStatusText(status)
}

const getRoleText = (roleCode: string): string => {
  return AdminUserService.getRoleText(roleCode)
}

const getStatusTagType = (status: number): string => {
  return AdminUserService.getStatusTagType(status)
}

const getRoleTagType = (roleCode: string): string => {
  return AdminUserService.getRoleTagType(roleCode)
}

const getToggleActionText = (status: number): string => {
  return AdminUserService.getToggleActionText(status)
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.admin-content {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.main-card {
  border-radius: 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 36px;
  height: 36px;
}

.empty-state {
  padding: 40px;
}
</style>