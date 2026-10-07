/**
 * 用户认证与账号绑定 API 函数
 *
 * <p>覆盖"我的-账号设置"所需的个人信息查询、微信绑定（二维码/状态轮询/确认）
 * 与密码设置接口，统一走 Axios 实例（token 附加、响应解包由拦截器统一处理）。</p>
 *
 * @author 傅崇睿
 */

import { mallInstance } from '../utils/axios'

/** 用户个人信息（与后端 UserProfileRes 字段一一对应） */
export interface UserProfile {
  id: number
  username: string
  status: number
  roleCode: string
  createTime: string
  updateTime: string
  /** 是否已绑定微信 */
  wechatBound: boolean
}

/** 微信绑定状态（与后端 UserBindStatusRes 字段一一对应，TD-1 起统一 camelCase） */
export interface BindStatusResult {
  status: 'BIND_SUCCESS' | 'BINDING_PENDING' | 'INVALID_CODE'
  openId?: string
}

/**
 * 获取当前登录用户个人信息
 * @returns 用户个人信息（含微信绑定状态）
 */
export const fetchProfile = (): Promise<UserProfile> => {
  return mallInstance.get('/profile') as Promise<UserProfile>
}

/**
 * 获取微信绑定二维码ticket
 * @returns 二维码ticket（用于拼接二维码图片URL）
 */
export const getBindQrCodeTicket = (): Promise<string> => {
  return mallInstance.get('/auth/bind/qrcode') as Promise<string>
}

/**
 * 轮询微信绑定状态
 * @param ticket 二维码ticket
 * @returns 绑定状态
 */
export const getBindStatus = (ticket: string): Promise<BindStatusResult> => {
  return mallInstance.get('/auth/bind/status', { params: { ticket } }) as Promise<BindStatusResult>
}

/**
 * 确认微信绑定（把扫码解析出的openId绑定到当前登录账号）
 * @param ticket 二维码ticket
 */
export const confirmBindWeChat = (ticket: string): Promise<unknown> => {
  return mallInstance.post('/auth/bind/confirm', { ticket })
}

/**
 * 设置/修改账户密码
 * @param password 明文密码
 */
export const setAccountPassword = (password: string): Promise<unknown> => {
  return mallInstance.post('/auth/password', { password })
}
