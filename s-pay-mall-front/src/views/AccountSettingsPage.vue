<script setup>
/**
 * 账号设置页：设置/修改账户密码、绑定/查看微信绑定状态
 *
 * <p>微信扫码注册的用户可在此补设密码（状态转为正常，账密可登录）；
 * 账密注册的用户可扫码绑定微信，绑定后微信扫码可直接登录同一账户。</p>
 *
 * @author 傅崇睿
 */

import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock, Message, Loading, CircleCheck } from '@element-plus/icons-vue'
import {
  fetchProfile,
  getBindQrCodeTicket,
  getBindStatus,
  confirmBindWeChat,
  setAccountPassword
} from '../api/auth'

const profile = ref(null)
const loading = ref(true)

/** 密码设置表单 */
const pwdFormRef = ref(null)
const pwdForm = reactive({
  password: '',
  confirmPassword: ''
})
const pwdRules = {
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度需在 6~64 位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}
const pwdSubmitting = ref(false)

/** 微信绑定弹窗 */
const bindDialogVisible = ref(false)
const bindQrTicket = ref('')
const bindQrCodeUrl = ref('')
const bindPolling = ref(false)
let bindTimer = null

const loadProfile = async () => {
  try {
    loading.value = true
    profile.value = await fetchProfile()
  } catch (error) {
    console.error('获取用户信息失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSetPassword = async () => {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    pwdSubmitting.value = true
    try {
      await setAccountPassword(pwdForm.password)
      ElMessage.success('密码设置成功，现在可以使用账号密码登录')
      pwdForm.password = ''
      pwdForm.confirmPassword = ''
      pwdFormRef.value.resetFields()
    } catch (error) {
      console.error('设置密码失败:', error)
    } finally {
      pwdSubmitting.value = false
    }
  })
}

const openBindDialog = async () => {
  bindDialogVisible.value = true
  bindQrTicket.value = ''
  bindQrCodeUrl.value = ''
  try {
    const ticket = await getBindQrCodeTicket()
    bindQrTicket.value = ticket
    bindQrCodeUrl.value = `https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=${encodeURIComponent(ticket)}`
    startBindPolling(ticket)
  } catch (error) {
    console.error('生成绑定二维码失败:', error)
  }
}

const startBindPolling = (ticket) => {
  stopBindPolling()
  bindPolling.value = true
  bindTimer = setInterval(async () => {
    try {
      const result = await getBindStatus(ticket)
      if (result.status === 'BIND_SUCCESS') {
        stopBindPolling()
        // 服务端按 ticket 解析 openId 完成落库绑定
        await confirmBindWeChat(ticket)
        ElMessage.success('微信绑定成功，现在可以扫码登录本账户')
        bindDialogVisible.value = false
        loadProfile()
      } else if (result.status === 'INVALID_CODE') {
        stopBindPolling()
      }
    } catch (error) {
      console.error('查询绑定状态失败:', error)
      stopBindPolling()
    }
  }, 3000)
}

const stopBindPolling = () => {
  if (bindTimer) {
    clearInterval(bindTimer)
    bindTimer = null
  }
  bindPolling.value = false
}

const handleBindDialogClose = () => {
  stopBindPolling()
}

onMounted(loadProfile)
onBeforeUnmount(stopBindPolling)
</script>

<template>
  <div class="account-settings-page">
    <h2 class="page-title">账号设置</h2>

    <!-- 密码设置 -->
    <div class="settings-card">
      <div class="card-header">
        <div class="card-header-icon blue">
          <el-icon><Lock /></el-icon>
        </div>
        <div>
          <h3>账户密码</h3>
          <p class="card-desc">设置密码后，除微信扫码外还可以使用账号密码登录</p>
        </div>
      </div>
      <el-form
        ref="pwdFormRef"
        :model="pwdForm"
        :rules="pwdRules"
        label-width="90px"
        class="pwd-form"
      >
        <el-form-item label="新密码" prop="password">
          <el-input
            v-model="pwdForm.password"
            type="password"
            show-password
            placeholder="请输入 6~64 位密码"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="pwdForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入密码"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="pwdSubmitting"
            @click="handleSetPassword"
          >
            {{ pwdSubmitting ? '提交中...' : '保存密码' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 微信绑定 -->
    <div class="settings-card">
      <div class="card-header">
        <div class="card-header-icon green">
          <el-icon><Message /></el-icon>
        </div>
        <div>
          <h3>微信绑定</h3>
          <p class="card-desc">绑定微信后，可直接使用微信扫码登录本账户</p>
        </div>
      </div>
      <div class="bind-body">
        <template v-if="profile">
          <div v-if="profile.wechatBound" class="bind-status bound">
            <el-icon class="status-icon"><CircleCheck /></el-icon>
            <span>已绑定微信，可使用微信扫码登录</span>
          </div>
          <div v-else class="bind-status unbound">
            <span>未绑定微信</span>
            <el-button type="success" @click="openBindDialog">扫码绑定微信</el-button>
          </div>
        </template>
      </div>
    </div>

    <!-- 微信绑定二维码弹窗 -->
    <el-dialog
      v-model="bindDialogVisible"
      title="扫码绑定微信"
      width="360px"
      align-center
      @close="handleBindDialogClose"
    >
      <div class="qrcode-wrapper">
        <div v-if="bindQrCodeUrl" class="qrcode-img">
          <img :src="bindQrCodeUrl" alt="微信绑定二维码" />
        </div>
        <div v-else class="qrcode-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在生成二维码...</span>
        </div>
        <p class="qrcode-tip">请使用微信扫描二维码完成绑定</p>
        <p v-if="bindPolling" class="qrcode-polling">等待扫码中...</p>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.account-settings-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px;
}

.settings-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
  padding: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.card-header-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.card-header-icon.blue {
  background: #ecf5ff;
  color: #409eff;
}

.card-header-icon.green {
  background: #f0f9eb;
  color: #67c23a;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px;
}

.card-desc {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.pwd-form {
  max-width: 420px;
}

.bind-body {
  padding: 4px 0;
}

.bind-status {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #606266;
}

.bind-status.bound {
  color: #67c23a;
}

.status-icon {
  font-size: 20px;
}

.bind-status.unbound {
  justify-content: space-between;
}

.qrcode-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qrcode-img {
  width: 220px;
  height: 220px;
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
  width: 220px;
  height: 220px;
  background: #f5f7fa;
  border-radius: 8px;
  color: #909399;
  font-size: 14px;
}

.qrcode-loading .el-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.qrcode-tip {
  color: #606266;
  font-size: 14px;
  margin: 16px 0 4px;
}

.qrcode-polling {
  color: #909399;
  font-size: 12px;
  margin: 0;
}
</style>
