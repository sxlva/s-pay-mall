<script setup>
/**
 * 个人中心页：用户信息、菜单导航、退出登录
 *
 * @author 傅崇睿
 */

import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Calendar, Setting, SwitchButton, ShoppingBag, Star, UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const profile = ref({})
const loading = ref(true)

const menuItems = [
  { icon: ShoppingBag, label: '我的订单', path: '/orders' },
  { icon: Star, label: '我的收藏', path: null },
  { icon: Setting, label: '账号设置', path: null }
]

const handleMenuClick = (item) => {
  if (item.path) {
    router.push(item.path)
  } else {
    ElMessage.info('核心答辩主流程之外的功能暂未开放')
  }
}

const loadProfile = async () => {
  try {
    loading.value = true
    // 从 localStorage 重新加载用户状态，确保角色信息正确
    userStore.reloadFromStorage()
    
    const token = userStore.token
    if (!token) {
      router.push('/login')
      return
    }
    const response = await fetch(`/mall-api/v1/profile`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    const data = await response.json()
    console.log('【用户信息】', data);
    // 后端返回的 code 是字符串 "0000"，不是数字 0
    if (data.code === '0000') {
      profile.value = data.data || {}
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  } finally {
    loading.value = false
  }
}

const logout = () => {
  // 使用统一的用户状态管理登出
  userStore.logout()
  router.push('/login')
}

const avatarColor = computed(() => {
  const colors = ['#667eea', '#764ba2', '#f093fb', '#4facfe', '#43e97b', '#fa709a', '#30cfd0', '#a8edea']
  const username = userStore.username || profile.value.username || 'user'
  let hash = 0
  for (let i = 0; i < username.length; i++) {
    hash = username.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
})

const avatarText = computed(() => {
  const username = userStore.username || profile.value.username || '用户'
  return username.substring(0, 2).toUpperCase()
})

const formattedCreateTime = computed(() => {
  if (!profile.value.createTime) return '-'
  const time = profile.value.createTime
  if (time.includes('T')) {
    return time.slice(0, 19).replace('T', ' ')
  }
  return time
})

onMounted(loadProfile)
</script>

<template>
  <div class="profile-page">
    <div class="profile-header">
      <div class="header-bg"></div>
      <div class="header-content">
        <el-avatar 
          class="avatar" 
          :size="80" 
          :style="{ background: avatarColor }"
        >
          <UserFilled class="avatar-icon" />
        </el-avatar>
        <div class="user-info">
          <!-- 使用统一的用户状态管理 -->
          <h2 class="username">{{ userStore.username || profile.username || '用户' }}</h2>
          <el-tag 
            :type="userStore.isAdmin ? 'danger' : 'primary'" 
            size="small"
          >
            {{ userStore.roleDisplayName }}
          </el-tag>
        </div>
      </div>
    </div>

    <div class="info-card">
      <div class="card-header">
        <h3>个人信息</h3>
      </div>
      <div class="info-list">
        <div class="info-item">
          <div class="info-icon purple">
            <el-icon><User /></el-icon>
          </div>
          <div class="info-content">
            <p class="info-label">用户权限</p>
            <!-- 使用统一的角色映射 -->
            <p class="info-value">{{ userStore.roleDisplayName }}</p>
          </div>
        </div>
        <div class="info-item">
          <div class="info-icon orange">
            <Calendar />
          </div>
          <div class="info-content">
            <p class="info-label">注册时间</p>
            <p class="info-value">{{ formattedCreateTime }}</p>
          </div>
        </div>
      </div>
    </div>

    <div class="menu-card">
      <div class="card-header">
        <h3>快捷功能</h3>
      </div>
      <div class="menu-list">
        <button
          v-for="item in menuItems"
          :key="item.label"
          class="menu-item"
          @click="handleMenuClick(item)"
        >
          <div class="menu-icon">
            <component :is="item.icon" />
          </div>
          <div class="menu-label">
            <p>{{ item.label }}</p>
          </div>
          <el-icon class="menu-arrow"><ArrowRight /></el-icon>
        </button>
        <button class="menu-item logout" @click="logout">
          <div class="menu-icon">
            <el-icon><SwitchButton /></el-icon>
          </div>
          <div class="menu-label">
            <p>退出登录</p>
          </div>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ArrowRight } from '@element-plus/icons-vue'
export default {
  components: { ArrowRight }
}
</script>

<style scoped>
.profile-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.profile-header {
  position: relative;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 32px;
  color: white;
  margin-bottom: 20px;
  overflow: hidden;
}

.header-bg {
  position: absolute;
  inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.05'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  opacity: 0.5;
}

.header-content {
  position: relative;
  display: flex;
  align-items: center;
  gap: 24px;
}

.avatar {
  border: 3px solid rgba(255, 255, 255, 0.3);
}

.avatar-icon {
  font-size: 32px;
  color: white;
}

.user-info {
  flex: 1;
}

.username {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px;
}

.info-card,
.menu-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
  overflow: hidden;
}

.card-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.info-list,
.menu-list {
  padding: 8px 0;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  transition: background 0.2s;
}

.info-item:hover {
  background: #f8f9fa;
}

.info-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.info-icon.purple {
  background: #f3e8ff;
  color: #722ed1;
}



.info-icon.orange {
  background: #fff7e6;
  color: #fa8c16;
}

.info-content {
  flex: 1;
}

.info-label {
  font-size: 13px;
  color: #909399;
  margin: 0 0 4px;
}

.info-value {
  font-size: 15px;
  color: #303133;
  margin: 0;
  font-weight: 500;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  text-decoration: none;
  color: inherit;
  transition: background 0.2s;
  width: 100%;
  border: none;
  background: none;
  cursor: pointer;
  text-align: left;
}

.menu-item:hover {
  background: #f8f9fa;
}

.menu-icon {
  width: 44px;
  height: 44px;
  background: #f5f5f5;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #606266;
}

.menu-label {
  flex: 1;
}

.menu-label p {
  font-size: 15px;
  color: #303133;
  margin: 0;
  font-weight: 500;
}

.menu-arrow {
  color: #c0c4cc;
  font-size: 16px;
}

.menu-item.logout {
  color: #f56c6c;
}

.menu-item.logout .menu-icon {
  background: #fef0f0;
  color: #f56c6c;
}

.menu-item.logout .menu-label p {
  color: #f56c6c;
}
</style>
