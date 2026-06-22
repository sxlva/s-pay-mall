/**
 * 用户状态管理 Store
 * 基于 DDD 值对象思想，统一管理用户身份信息
 *
 * @author 傅崇睿
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  // 状态定义
  const userId = ref<string | null>(localStorage.getItem('userId') || null)
  const username = ref<string | null>(localStorage.getItem('username') || null)
  const role = ref<string | null>(localStorage.getItem('role') || null)
  const token = ref<string | null>(localStorage.getItem('token') || null)

  // 角色显示映射（值对象映射）
  const roleDisplayMap: Record<string, string> = {
    ADMIN: '管理员',
    MEMBER: '普通会员',
    VIP: 'VIP会员',
    GUEST: '普通用户'
  }

  /**
   * 计算属性：获取角色显示名称
   * 确保角色值对象正确映射
   */
  const roleDisplayName = computed(() => {
    if (!role.value) return '普通用户'
    return roleDisplayMap[role.value] || role.value
  })

  /**
   * 计算属性：判断是否为管理员
   */
  const isAdmin = computed(() => role.value === 'ADMIN')

  /**
   * 登录方法：保存用户信息到状态和 localStorage
   */
  const login = (userData: { userId: string; username: string; role: string; token: string }) => {
    userId.value = userData.userId
    username.value = userData.username
    role.value = userData.role
    token.value = userData.token
    
    // 持久化到 localStorage
    localStorage.setItem('userId', userData.userId)
    localStorage.setItem('username', userData.username)
    localStorage.setItem('role', userData.role)
    localStorage.setItem('token', userData.token)
  }

  /**
   * 登出方法：清除用户信息
   */
  const logout = () => {
    userId.value = null
    username.value = null
    role.value = null
    token.value = null
    
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    localStorage.removeItem('token')
  }

  /**
   * 从 localStorage 重新加载用户信息
   * 用于页面刷新后的状态恢复
   */
  const reloadFromStorage = () => {
    userId.value = localStorage.getItem('userId') || null
    username.value = localStorage.getItem('username') || null
    role.value = localStorage.getItem('role') || null
    token.value = localStorage.getItem('token') || null
  }

  return {
    userId,
    username,
    role,
    token,
    roleDisplayName,
    isAdmin,
    login,
    logout,
    reloadFromStorage
  }
})
