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
        <el-table-column prop="roleCode" label="角色" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.roleCode === 'ADMIN' ? 'danger' : 'success'">
              {{ scope.row.roleCode === 'ADMIN' ? '管理员' : '普通会员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'warning'">
              {{ scope.row.status === 1 ? '正常' : '禁用' }}
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
              {{ scope.row.status === 1 ? '封禁' : '解封' }}
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

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import { getAdminUsers, updateAdminUserStatus, deleteAdminUser } from '../../api/admin'

const users = ref([])

const load = async () => {
  try {
    const data = await getAdminUsers()
    users.value = data || []
  } catch (error) {
    console.error('获取用户列表失败:', error)
  }
}

const handleToggleStatus = async (row) => {
  const action = row.status === 1 ? '封禁' : '解封'
  try {
    await ElMessageBox.confirm(
      `确定要${action}该用户吗？`,
      '提示',
      { type: 'warning' }
    )

    const newStatus = row.status === 1 ? 0 : 1
    await updateAdminUserStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    await load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

const handleDelete = async (row) => {
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

load()
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
