/**
 * Axios 实例配置：请求/响应拦截器，统一处理 token 和错误
 *
 * @author 傅崇睿
 */

import axios from 'axios'
import type { AxiosInstance } from 'axios'
import { ElMessage } from 'element-plus'

/** 商城端 Axios 实例（baseURL: /mall-api/v1） */
const mallInstance: AxiosInstance = axios.create({
  baseURL: '/mall-api/v1',
  timeout: 10000
})

/** 管理端 Axios 实例（baseURL: /mall-api/v1） */
const adminInstance: AxiosInstance = axios.create({
  baseURL: '/mall-api/v1',
  timeout: 10000
})

/**
 * 设置请求拦截器：自动附加 Bearer token
 * @param instance Axios 实例
 */
const setupRequestInterceptor = (instance: AxiosInstance) => {
  instance.interceptors.request.use(
    (config) => {
      const token = localStorage.getItem('token')
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    },
    (error) => {
      return Promise.reject(error)
    }
  )
}

/**
 * 登录态失效统一处理（S-02 配套）：清理本地登录态并跳转登录页。
 * 触发场景：HTTP 401（Security 入口点，code 0003）或业务响应 code 0003（未登录）。
 * 已在登录页时不跳转，避免循环。
 */
const handleSessionExpired = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('role')
  if (!window.location.pathname.startsWith('/login')) {
    window.location.assign('/login')
  }
}

/**
 * 设置响应拦截器：统一处理成功/错误响应，提取 data 或弹出错误
 * @param instance Axios 实例
 */
const setupResponseInterceptor = (instance: AxiosInstance) => {
  instance.interceptors.response.use(
    (response) => {
      const data = response.data
      // 后端成功状态码是字符串 "0000"，同时兼容数字 0 和 200
      const successCodes: (string | number)[] = ['0000', '0', 0, 200, '200']
      if (successCodes.includes(data.code)) {
        // 成功响应，不弹出提示（避免频繁弹窗）
        return data.data
      }
      // 登录态失效（业务通道 0003，如 Controller 内解析兜底返回）
      if (data.code === '0003') {
        ElMessage.warning('登录已过期，请重新登录')
        handleSessionExpired()
        return Promise.reject(new Error(data.info || '未登录'))
      }
      // 业务错误，弹出错误提示
      ElMessage.error(data.info || data.msg || '请求失败')
      return Promise.reject(new Error(data.info || data.msg || '请求失败'))
    },
    (error) => {
      // S-02：HTTP 401 = Security 入口点拦截（未登录/非法 token），清登录态并跳登录页
      if (error.response?.status === 401 || error.response?.data?.code === '0003') {
        ElMessage.warning('登录已过期，请重新登录')
        handleSessionExpired()
        return Promise.reject(error)
      }
      // HTTP 错误或网络错误
      const errorMsg = error.response?.data?.info || error.response?.data?.msg || error.message || '请求失败'
      ElMessage.error(errorMsg)
      return Promise.reject(error)
    }
  )
}

// 为两个实例都设置拦截器
setupRequestInterceptor(mallInstance)
setupResponseInterceptor(mallInstance)
setupRequestInterceptor(adminInstance)
setupResponseInterceptor(adminInstance)

export { mallInstance, adminInstance }
export default mallInstance
