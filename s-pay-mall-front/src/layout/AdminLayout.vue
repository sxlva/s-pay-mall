<template>
  <el-container class="admin-container">
    <el-aside width="220px" class="admin-aside">
      <div class="logo">
        <el-icon class="logo-icon"><Shop /></el-icon>
        <span class="logo-text">管理后台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="admin-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item index="/admin">
          <el-icon><HomeFilled /></el-icon>
          <span>系统首页</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/categories">
          <el-icon><Folder /></el-icon>
          <span>分类管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/products">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><ShoppingCart /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="header-left">
          <h3 class="page-title">{{ pageTitle }}</h3>
        </div>
        <div class="header-right">
          <el-button type="info" link icon="Back" @click="goHome">
            返回商城
          </el-button>
          <el-dropdown @command="handleCommand">
            <el-button type="text" class="user-btn">
              <el-icon><User /></el-icon>
              {{ username }}
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
/**
 * 管理后台布局组件：左侧菜单 + 顶部导航 + 内容区
 *
 * @author 傅崇睿
 */

import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Shop, 
  HomeFilled,
  User, 
  Goods, 
  ShoppingCart, 
  Folder 
} from '@element-plus/icons-vue'

const router = useRouter()

const activeMenu = computed(() => {
  return router.currentRoute.value.path
})

const username = computed(() => {
  return localStorage.getItem('username') || ''
})

const pageTitleMap = {
  '/admin': '系统首页',
  '/admin/users': '用户管理',
  '/admin/categories': '分类管理',
  '/admin/products': '商品管理',
  '/admin/orders': '订单管理'
}

const pageTitle = computed(() => {
  return pageTitleMap[activeMenu.value] || '管理后台'
})

const handleMenuSelect = (index) => {
  router.push(index)
}

const goHome = () => {
  router.push('/')
}

const handleCommand = (command) => {
  if (command === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    ElMessage.success('退出成功')
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped>
.admin-container {
  height: 100vh;
}

.admin-aside {
  background: #2f4050;
  color: #fff;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 60px;
  border-bottom: 1px solid #293846;
}

.logo-icon {
  font-size: 24px;
  color: #409EFF;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}

.admin-menu {
  border: none;
  background: #2f4050;
}

.admin-menu :deep(.el-menu-item) {
  color: #a7b1c2;
  border-right: 3px solid transparent;
  height: 56px;
  line-height: 56px;
}

.admin-menu :deep(.el-menu-item:hover) {
  background: #293846;
  color: #fff;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: #1ab394;
  border-right-color: #1ab394;
  color: #fff;
}

.admin-header {
  background: #fff;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-btn {
  padding: 0 12px;
  color: #606266;
}

.user-btn:hover {
  color: #409EFF;
}

.admin-main {
  padding: 20px;
  background: #f3f3f4;
  overflow-y: auto;
}
</style>
