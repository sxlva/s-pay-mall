/**
 * 管理员后台-用户管理 API 函数
 *
 * <p>FP0-2：由 api/admin.ts 按业务拆分迁入。</p>
 *
 * @author 傅崇睿
 */

import { adminInstance } from '../../utils/axios'
import type { UserAdminVO, UserAdminQueryParams } from '../../types/domain/admin'
import type { SaveUserParams } from '../../types/api/admin'

/**
 * 获取管理员用户列表
 * @param params 查询参数
 * @returns 用户列表
 */
export const getAdminUsers = (params: UserAdminQueryParams = {}): Promise<UserAdminVO[]> => {
  return adminInstance.get('/admin/users', { params })
}

/**
 * 创建或更新管理员用户
 * @param data 用户数据
 * @returns 保存后的用户数据
 */
export const saveAdminUser = (data: SaveUserParams): Promise<UserAdminVO> => {
  return adminInstance.post('/admin/users', data)
}

/**
 * 更新用户状态（封禁/解封）
 * @param id 用户 ID
 * @param status 新状态值：1-正常 0-禁用
 * @returns 更新后的用户数据
 */
export const updateAdminUserStatus = (id: number, status: number): Promise<UserAdminVO> => {
  return adminInstance.put(`/admin/users/${id}/status`, null, { params: { status } })
}

/**
 * 删除管理员用户
 * @param id 用户 ID
 * @returns void
 */
export const deleteAdminUser = (id: number): Promise<void> => {
  return adminInstance.delete(`/admin/users/${id}`)
}
