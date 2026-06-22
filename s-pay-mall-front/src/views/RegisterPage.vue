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
/**
 * 注册页：用户名注册表单
 *
 * @author 傅崇睿
 */

import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)

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
</style>
