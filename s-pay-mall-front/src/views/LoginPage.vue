<template>
  <div class="login-container">
    <div class="login-left">
      <div class="brand-section">
        <el-icon class="brand-icon"><Shop /></el-icon>
        <h1 class="brand-title">S-Pay Mall</h1>
        <p class="brand-subtitle">聚合支付商城</p>
      </div>

      <div class="features-section">
        <div class="feature-item">
          <el-icon class="feature-icon" color="#409EFF"><Lock /></el-icon>
          <span>安全可靠的支付体系</span>
        </div>
        <div class="feature-item">
          <el-icon class="feature-icon" color="#67C23A"><Lightning /></el-icon>
          <span>极速响应的服务体验</span>
        </div>
        <div class="feature-item">
          <el-icon class="feature-icon" color="#909399"><Key /></el-icon>
          <span>隐私保护全程护航</span>
        </div>
      </div>
    </div>

    <div class="login-right">
      <el-card class="login-card" shadow="always">
        <template #header>
          <div class="card-header">
            <h2>欢迎回来</h2>
            <p>请登录您的账户</p>
          </div>
        </template>

        <el-form
          ref="loginFormRef"
          :model="form"
          :rules="rules"
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              clearable
            />
          </el-form-item>

          <el-form-item>
            <div class="form-options">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              <el-link type="primary" :underline="false">忘记密码？</el-link>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              :loading="isLoading"
              native-type="submit"
            >
              {{ isLoading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>

          <el-divider>
            <span class="divider-text">其他登录方式</span>
          </el-divider>

          <el-form-item>
            <el-button
              type="success"
              size="large"
              class="wechat-btn"
              @click="toggleWechatLogin"
            >
              <el-icon class="mr-2"><Message /></el-icon>
              微信扫码登录
            </el-button>
          </el-form-item>

          <transition name="el-zoom-in-top">
            <div v-if="showWechatQrCode" class="qrcode-section">
              <div class="qrcode-header">
                <span>扫码登录</span>
                <el-button text @click="showWechatQrCode = false">
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>
              <div class="qrcode-content">
                <div v-if="qrCodeUrl" class="qrcode-img">
                  <img :src="qrCodeUrl" alt="微信登录二维码" />
                </div>
                <div v-else class="qrcode-loading">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  <span>正在生成二维码...</span>
                </div>
              </div>
              <p class="qrcode-tip">使用微信扫描二维码登录</p>
            </div>
          </transition>
        </el-form>

        <div class="register-link">
          <span>还没有账户？</span>
          <router-link to="/register">
            <el-link type="primary" :underline="false">立即注册</el-link>
          </router-link>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Shop,
  Lock,
  Lightning,
  Key,
  User,
  Message,
  Close,
  Loading
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const loginFormRef = ref(null)
const form = reactive({
  username: '',
  password: ''
})
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const rememberMe = ref(false)
const isLoading = ref(false)
const showWechatQrCode = ref(false)
const qrCodeUrl = ref('')

onMounted(() => {
  const username = route.query.username
  if (username) {
    form.username = username
  }
})

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    isLoading.value = true
    try {
      const response = await fetch('/mall-api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: form.username,
          password: form.password
        })
      })
      const data = await response.json()
      // 后端返回的 code 是字符串 "0000"，不是数字 0
      if (data.code === '0000') {
        localStorage.setItem('token', data.data.token)
        localStorage.setItem('userId', String(data.data.userId))
        localStorage.setItem('username', data.data.username)
        localStorage.setItem('role', data.data.role)
        ElMessage.success('登录成功')
        router.push('/')
      } else {
        ElMessage.error(data.info || '登录失败')
      }
    } catch (error) {
      console.error('Login error:', error)
      ElMessage.error('登录失败，请稍后重试')
    } finally {
      isLoading.value = false
    }
  })
}

const toggleWechatLogin = () => {
  showWechatQrCode.value = !showWechatQrCode.value
  if (showWechatQrCode.value && !qrCodeUrl.value) {
    generateQrCode()
  }
}

const generateQrCode = async () => {
  try {
    const response = await fetch('/pay-api/v1/login/weixin_qrcode_ticket')
    const data = await response.json()
    if (data.code === 0 && data.data) {
      qrCodeUrl.value = `https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=${encodeURIComponent(data.data)}`
      startPolling(data.data)
    }
  } catch (error) {
    console.error('Failed to generate QR code:', error)
  }
}

const startPolling = (ticket) => {
  const interval = setInterval(async () => {
    try {
      const response = await fetch(`/pay-api/v1/login/check_login?ticket=${ticket}`)
      const data = await response.json()
      if (data.code === 0 && data.data) {
        localStorage.setItem('token', data.data)
        localStorage.setItem('username', 'wechat_user')
        localStorage.setItem('role', 'MEMBER')
        clearInterval(interval)
        ElMessage.success('微信登录成功')
        window.location.href = '/'
      }
    } catch (error) {
      console.error('Polling error:', error)
    }
  }, 3000)
}
</script>

<style scoped>
.login-container {
  display: flex;
  width: 100%;
  height: 100vh;
  min-height: 100vh;
  margin: 0;
  padding: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-left {
  flex: 1;
  display: none;
  padding: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

@media (min-width: 992px) {
  .login-left {
    display: flex;
    flex-direction: column;
    justify-content: center;
  }
}

.brand-section {
  margin-bottom: 60px;
}

.brand-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.brand-title {
  font-size: 48px;
  font-weight: 700;
  margin-bottom: 12px;
}

.brand-subtitle {
  font-size: 20px;
  opacity: 0.9;
}

.features-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 16px;
}

.feature-icon {
  font-size: 24px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
}

.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #f5f7fa;
}

.login-card {
  width: 100%;
  max-width: 420px;
  border-radius: 16px;
}

.card-header {
  text-align: center;
}

.card-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.card-header p {
  color: #909399;
  font-size: 14px;
}

.login-form {
  margin-top: 24px;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #409EFF 0%, #337ecc 100%);
  border: none;
  border-radius: 8px;
}

.login-btn:hover {
  background: linear-gradient(135deg, #66b1ff 0%, #409EFF 100%);
}

.wechat-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 8px;
}

.divider-text {
  color: #909399;
  font-size: 12px;
}

.qrcode-section {
  margin-top: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 12px;
  border: 1px solid #ebeef5;
}

.qrcode-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-weight: 500;
  color: #303133;
}

.qrcode-content {
  display: flex;
  justify-content: center;
  margin-bottom: 12px;
}

.qrcode-img {
  width: 192px;
  height: 192px;
  padding: 8px;
  background: white;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.qrcode-img img {
  width: 100%;
  height: 100%;
}

.qrcode-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 192px;
  height: 192px;
  background: white;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  color: #909399;
  font-size: 14px;
}

.qrcode-loading .el-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.qrcode-tip {
  text-align: center;
  color: #909399;
  font-size: 12px;
  margin: 0;
}

.register-link {
  margin-top: 24px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}

.register-link a {
  margin-left: 4px;
}

.mr-2 {
  margin-right: 8px;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-card__header) {
  padding: 24px;
  border-bottom: none;
}

:deep(.el-card__body) {
  padding: 0 24px 24px;
}
</style>
