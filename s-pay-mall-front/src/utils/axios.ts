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
      // 业务错误，弹出错误提示
      ElMessage.error(data.info || data.msg || '请求失败')
      return Promise.reject(new Error(data.info || data.msg || '请求失败'))
    },
    (error) => {
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
