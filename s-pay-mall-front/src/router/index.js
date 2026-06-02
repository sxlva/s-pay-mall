// 路由配置：定义页面访问路径与权限守卫
import { createRouter, createWebHistory } from 'vue-router'

import Layout from '../layout/Layout.vue'
import HomePage from '../views/HomePage.vue'
import LoginPage from '../views/LoginPage.vue'
import RegisterPage from '../views/RegisterPage.vue'
import ProductListPage from '../views/ProductListPage.vue'
import ProductDetailPage from '../views/ProductDetailPage.vue'
import CartPage from '../views/CartPage.vue'
import CheckoutPage from '../views/CheckoutPage.vue'
import OrderListPage from '../views/OrderListPage.vue'
import ProfilePage from '../views/ProfilePage.vue'
import AdminLayout from '../layout/AdminLayout.vue'
import AdminDashboardPage from '../views/admin/AdminDashboardPage.vue'
import AdminUsersPage from '../views/admin/AdminUsersPage.vue'
import AdminCategoriesPage from '../views/admin/AdminCategoriesPage.vue'
import AdminProductsPage from '../views/admin/AdminProductsPage.vue'
import AdminOrdersPage from '../views/admin/AdminOrdersPage.vue'

const routes = [
  // 登录/注册页面（独立，无布局）
  { path: '/login', component: LoginPage },
  { path: '/register', component: RegisterPage },
  
  // 前台页面（使用统一布局）
  {
    path: '/',
    component: Layout,
    children: [
      { path: '', component: HomePage },
      { path: 'products', component: ProductListPage },
      { path: 'products/:id', component: ProductDetailPage },
      { path: 'cart', component: CartPage, meta: { requiresAuth: true } },
      { path: 'checkout', component: CheckoutPage, meta: { requiresAuth: true } },
      { path: 'orders', component: OrderListPage, meta: { requiresAuth: true } },
      { path: 'profile', component: ProfilePage, meta: { requiresAuth: true } }
    ]
  },
  
  // 后台管理页面（使用 AdminLayout 布局，左侧菜单常驻）
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      { path: '', component: AdminDashboardPage },
      { path: 'users', component: AdminUsersPage },
      { path: 'categories', component: AdminCategoriesPage },
      { path: 'products', component: AdminProductsPage },
      { path: 'orders', component: AdminOrdersPage }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  console.log('【路由守卫】正在导航到:', to.path)
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')
  
  console.log('【路由守卫】token:', token ? '存在' : '不存在', ', role:', role)
  
  // 白名单：登录和注册不需要 Token
  if (to.path === '/login' || to.path === '/register') {
    console.log('【路由守卫】白名单路径，直接放行')
    next()
    return
  }
  
  // 其余任何页面，只要没有 Token，一律强制重定向到 /login
  if (!token) {
    console.log('【路由守卫】没有token，重定向到 /login')
    next('/login')
    return
  }
  
  // 需要特定角色但角色不匹配
  if (to.meta.role && to.meta.role !== role) {
    console.log('【路由守卫】角色不匹配，重定向到 /')
    next('/')
    return
  }
  
  console.log('【路由守卫】正常放行')
  next()
})

export default router