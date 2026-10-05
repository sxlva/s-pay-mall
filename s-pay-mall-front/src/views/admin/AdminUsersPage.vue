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
/**
 * 管理员用户管理页面
 *
 * @author 傅崇睿
 */
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import { getAdminUsers, updateAdminUserStatus, deleteAdminUser } from '../../api/admin/user'
import type { UserAdminVO } from '../../types/domain/admin'

const users = ref<UserAdminVO[]>([])

const load = async () => {
  try {
    users.value = await getAdminUsers()
  } catch (error) {
    console.error('获取用户列表失败:', error)
  }
}

const handleToggleStatus = async (row: UserAdminVO) => {
  const action = getToggleActionText(row.status)
  try {
    await ElMessageBox.confirm(
      `确定要${action}该用户吗？`,
      '提示',
      { type: 'warning' }
    )

    await updateAdminUserStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(`${action}成功`)
    await load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

const handleDelete = async (row: UserAdminVO) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该用户吗？此操作不可恢复！',
      '警告',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )

    await deleteAdminUser(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/** 状态显示文本：1-正常 其他-禁用 */
const getStatusText = (status: number): string => {
  return status === 1 ? '正常' : '禁用'
}

/** 角色显示文本映射（展示规则随页面内聚） */
const getRoleText = (roleCode: string): string => {
  const roleMap: Record<string, string> = {
    ADMIN: '管理员',
    MEMBER: '普通会员'
  }
  return roleMap[roleCode] || roleCode
}

/** 状态标签类型 */
const getStatusTagType = (status: number): string => {
  return status === 1 ? 'success' : 'warning'
}

/** 角色标签类型 */
const getRoleTagType = (roleCode: string): string => {
  return roleCode === 'ADMIN' ? 'danger' : 'success'
}

/** 操作文本：封禁/解封 */
const getToggleActionText = (status: number): string => {
  return status === 1 ? '封禁' : '解封'
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