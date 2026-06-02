<template>
  <div class="register-container">
    <el-card class="register-card" shadow="always">
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><UserFilled /></el-icon>
          <h2>创建账户</h2>
          <p>填写信息以开始您的购物之旅</p>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="submit"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="3-20位字母、数字或下划线"
            maxlength="20"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="6-20位密码"
            maxlength="20"
            :prefix-icon="Lock"
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入密码"
            maxlength="20"
            :prefix-icon="Lock"
          />
        </el-form-item>

        <!-- 微信绑定区域 -->
        <el-form-item label="微信公众号绑定（可选）">
          <div class="wechat-bind-section">
            <div v-if="!bindCode" class="bind-code-empty">
              <el-button
                type="primary"
                size="default"
                icon="Message"
                @click="generateBindCode"
                :loading="generatingCode"
              >
                获取绑定码
              </el-button>
              <span class="bind-tip">绑定后可使用微信扫码登录</span>
            </div>
            
            <div v-else class="bind-code-container">
              <div class="bind-code-display">
                <span class="code-label">绑定码：</span>
                <span class="code-value">{{ bindCode }}</span>
                <el-button
                  type="text"
                  icon="Refresh"
                  @click="generateBindCode"
                  :loading="generatingCode"
                >
                  刷新
                </el-button>
              </div>
              
              <div class="bind-instruction">
                <p class="instruction-title">请在微信公众号完成绑定：</p>
                <ol>
                  <li>打开微信，关注公众号</li>
                  <li>发送消息：<strong>绑定 {{ bindCode }}</strong></li>
                  <li>等待系统确认...</li>
                </ol>
              </div>
              
              <div class="bind-status">
                <el-progress
                  type="circle"
                  :percentage="bindProgress"
                  :size="60"
                  :status="bindStatusColor"
                />
                <span :class="['status-text', bindStatusClass]">{{ bindStatusText }}</span>
              </div>
              
              <el-button
                v-if="bindSuccess"
                type="success"
                size="small"
                icon="Check"
                disabled
              >
                绑定成功
              </el-button>
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="register-btn" native-type="submit" :loading="submitting">
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-link">
        <span>已有账号？</span>
        <router-link to="/login">
          <el-link type="primary" :underline="false">去登录</el-link>
        </router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, UserFilled, Message, Refresh, Check } from '@element-plus/icons-vue'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const generatingCode = ref(false)
const bindCode = ref('')
const bindStatus = ref('') // BINDING_PENDING, BIND_SUCCESS, INVALID_CODE
const bindSuccess = ref(false)
const pollingTimer = ref(null)
const openId = ref('')

const form = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '用户名不能为空', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度必须在3-20位之间', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '密码不能为空', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须在6-20位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validatePass2, trigger: 'blur' }
  ]
}

const bindProgress = computed(() => {
  if (bindSuccess.value) return 100
  if (bindStatus.value === 'BINDING_PENDING') return 50
  if (bindStatus.value === 'INVALID_CODE') return 0
  return 0
})

const bindStatusColor = computed(() => {
  if (bindSuccess.value) return 'success'
  if (bindStatus.value === 'BINDING_PENDING') return 'warning'
  if (bindStatus.value === 'INVALID_CODE') return 'exception'
  return 'info'
})

const bindStatusClass = computed(() => {
  if (bindSuccess.value) return 'success'
  if (bindStatus.value === 'BINDING_PENDING') return 'pending'
  if (bindStatus.value === 'INVALID_CODE') return 'error'
  return ''
})

const bindStatusText = computed(() => {
  if (bindSuccess.value) return '已绑定'
  if (bindStatus.value === 'BINDING_PENDING') return '等待绑定...'
  if (bindStatus.value === 'INVALID_CODE') return '绑定码已过期'
  return ''
})

const generateBindCode = async () => {
  generatingCode.value = true
  try {
    const response = await fetch('/mall-api/v1/auth/bind-code', {
      method: 'GET'
    })
    const data = await response.json()
    if (data.code === 0 && data.data) {
      bindCode.value = data.data.bindCode
      bindStatus.value = 'BINDING_PENDING'
      bindSuccess.value = false
      openId.value = ''
      startPolling()
    } else {
      ElMessage.error('获取绑定码失败')
    }
  } catch (error) {
    ElMessage.error('获取绑定码失败')
  } finally {
    generatingCode.value = false
  }
}

const startPolling = () => {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value)
  }
  
  pollingTimer.value = setInterval(async () => {
    try {
      const response = await fetch(`/mall-api/v1/auth/check-bind-status?ticket=${bindCode.value}`, {
        method: 'GET'
      })
      const data = await response.json()
      if (data.code === 0 && data.data) {
        const status = data.data.status
        if (status === 'BIND_SUCCESS') {
          bindStatus.value = 'BIND_SUCCESS'
          bindSuccess.value = true
          openId.value = data.data.openId
          stopPolling()
          ElMessage.success('微信绑定成功！')
        } else if (status === 'INVALID_CODE') {
          bindStatus.value = 'INVALID_CODE'
          stopPolling()
        }
      }
    } catch (error) {
      console.error('轮询绑定状态失败:', error)
    }
  }, 3000)
}

const stopPolling = () => {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value)
    pollingTimer.value = null
  }
}

const submit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      const requestData = {
        username: form.username,
        password: form.password
      }
      
      if (openId.value) {
        requestData.openId = openId.value
      }

      const response = await fetch('/mall-api/v1/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestData)
      })
      const data = await response.json()
      console.log('【注册接口返回数据】', data);
      if (data.code === '0000' || data.code === 0 || data.code === '0') {
        ElMessage.success('注册成功，正在跳转至登录页...')
        
        if (formRef.value) {
          formRef.value.resetFields()
        }
        
        bindCode.value = ''
        bindStatus.value = ''
        bindSuccess.value = false
        openId.value = ''
        stopPolling()
        
        setTimeout(() => {
          router.push({ path: '/login', query: { username: form.username } })
        }, 1500)
      } else {
        ElMessage.error(data.info || '注册失败')
      }
    } catch (error) {
      ElMessage.error('注册失败，用户名可能已存在')
    } finally {
      submitting.value = false
    }
  })
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-card {
  width: 100%;
  max-width: 480px;
  border-radius: 16px;
}

.card-header {
  text-align: center;
}

.header-icon {
  font-size: 48px;
  color: #409EFF;
  margin-bottom: 16px;
}

.card-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
}

.card-header p {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.register-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #409EFF 0%, #337ecc 100%);
  border: none;
  border-radius: 8px;
}

.register-btn:hover {
  background: linear-gradient(135deg, #66b1ff 0%, #409EFF 100%);
}

.login-link {
  margin-top: 20px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}

.login-link a {
  margin-left: 4px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-card__header) {
  padding: 30px;
  border-bottom: none;
}

:deep(.el-card__body) {
  padding: 0 30px 30px;
}

/* 微信绑定区域样式 */
.wechat-bind-section {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.bind-code-empty {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bind-tip {
  font-size: 13px;
  color: #909399;
}

.bind-code-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bind-code-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

.code-label {
  font-size: 14px;
  color: #606266;
}

.code-value {
  font-size: 16px;
  font-weight: 600;
  color: #409EFF;
  font-family: 'Courier New', monospace;
  letter-spacing: 2px;
}

.bind-instruction {
  background: #fff;
  padding: 12px;
  border-radius: 6px;
}

.instruction-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin: 0 0 8px;
}

.bind-instruction ol {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  color: #606266;
}

.bind-instruction li {
  margin-bottom: 4px;
}

.bind-instruction strong {
  color: #409EFF;
}

.bind-status {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-text {
  font-size: 14px;
}

.status-text.success {
  color: #67c23a;
}

.status-text.pending {
  color: #e6a23c;
}

.status-text.error {
  color: #f56c6c;
}
</style>