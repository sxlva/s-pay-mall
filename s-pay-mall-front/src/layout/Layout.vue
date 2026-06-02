<template>
  <el-container class="layout-container">
    <el-header class="header">
      <el-menu
        mode="horizontal"
        :ellipsis="false"
        class="header-menu"
        :default-active="activeIndex"
        @select="handleMenuSelect"
      >
        <div class="logo">
          <el-icon class="logo-icon"><Shop /></el-icon>
          <span class="logo-text">S-Pay Mall</span>
        </div>

        <div class="flex-grow"></div>

        <el-menu-item index="1">
          <el-icon><House /></el-icon>
          首页
        </el-menu-item>
        <el-menu-item index="2">
          <el-icon><Goods /></el-icon>
          商品
        </el-menu-item>
        <el-menu-item index="3">
          <el-icon><ShoppingCart /></el-icon>
          购物车
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="admin">
          <el-icon><Setting /></el-icon>
          管理后台
        </el-menu-item>
        <el-menu-item index="4">
          <el-icon><User /></el-icon>
          我的
        </el-menu-item>
        
        <template v-if="isLoggedIn">
          <el-dropdown>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goProfile">个人中心</el-dropdown-item>
                <el-dropdown-item @click="goOrders">我的订单</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
            <el-button type="text" class="user-btn">
              <el-icon><User /></el-icon>
              {{ username }}
            </el-button>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" text @click="goLogin">登录</el-button>
          <el-button text @click="goRegister">注册</el-button>
        </template>
      </el-menu>
    </el-header>

    <el-main class="main-content">
      <router-view />
    </el-main>

    <el-footer class="footer">
      <p>&copy; 2026 S-Pay-Mall FuChongRui. 保留所有权利。</p>
    </el-footer>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Shop,
  House,
  Goods,
  ShoppingCart,
  User,
  Setting
} from '@element-plus/icons-vue'

const router = useRouter()
const activeIndex = ref('1')

const isLoggedIn = computed(() => {
  return localStorage.getItem('token') !== null
})

const username = computed(() => {
  return localStorage.getItem('username') || ''
})

const isAdmin = computed(() => {
  return localStorage.getItem('role') === 'ADMIN'
})

const handleMenuSelect = (index) => {
  activeIndex.value = index
  switch (index) {
    case '1':
      router.push('/')
      break
    case '2':
      router.push('/products')
      break
    case '3':
      router.push('/cart')
      break
    case 'admin':
      router.push('/admin')
      break
    case '4':
      router.push('/profile')
      break
  }
}

const goLogin = () => {
  router.push('/login')
}

const goRegister = () => {
  router.push('/register')
}

const goProfile = () => {
  router.push('/profile')
}

const goOrders = () => {
  router.push('/orders')
}

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('role')
  ElMessage.success('退出成功')
  router.push('/login')
}

const updateActiveIndex = () => {
  const path = router.currentRoute.value.path
  if (path === '/' || path === '/home') {
    activeIndex.value = '1'
  } else if (path.startsWith('/products')) {
    activeIndex.value = '2'
  } else if (path.startsWith('/cart')) {
    activeIndex.value = '3'
  } else if (path.startsWith('/profile') || path.startsWith('/orders')) {
    activeIndex.value = '4'
  }
}

onMounted(() => {
  updateActiveIndex()
  router.afterEach(updateActiveIndex)
})
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  padding: 0;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-menu {
  max-width: 1400px;
  margin: 0 auto;
  border: none;
  height: 60px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 60px;
}

.logo-icon {
  font-size: 28px;
  color: #409EFF;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.flex-grow {
  flex-grow: 1;
}

.user-btn {
  padding: 0 12px;
}

.main-content {
  flex: 1;
  padding: 20px;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}

.footer {
  background: #303133;
  color: #909399;
  text-align: center;
  padding: 24px;
  font-size: 14px;
}

.footer p {
  margin: 0;
}

:deep(.el-menu-item) {
  margin: 0 8px;
}
</style>
