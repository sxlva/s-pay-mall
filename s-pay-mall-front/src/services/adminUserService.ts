import { getAdminUsers, updateAdminUserStatus, deleteAdminUser } from '../api/admin'
import type { UserVO, UserQueryParams } from '../types/adminUser'

/**
 * 管理员用户服务
 * 负责用户管理相关的业务编排逻辑
 */
export class AdminUserService {
  /**
   * 获取用户列表
   * @param params 查询参数
   * @returns 用户列表
   */
  static async listUsers(params: UserQueryParams = {}): Promise<UserVO[]> {
    try {
      const data = await getAdminUsers(params)
      return data || []
    } catch (error) {
      console.error('获取用户列表失败:', error)
      throw error
    }
  }

  /**
   * 切换用户状态（封禁/解封）
   * @param userId 用户ID
   * @param currentStatus 当前状态
   * @returns 是否成功
   */
  static async toggleUserStatus(userId: number, currentStatus: number): Promise<boolean> {
    const newStatus = currentStatus === 1 ? 0 : 1
    try {
      await updateAdminUserStatus(userId, newStatus)
      return true
    } catch (error) {
      console.error(`切换用户状态失败, userId=${userId}:`, error)
      throw error
    }
  }

  /**
   * 删除用户
   * @param userId 用户ID
   * @returns 是否成功
   */
  static async removeUser(userId: number): Promise<boolean> {
    try {
      await deleteAdminUser(userId)
      return true
    } catch (error) {
      console.error(`删除用户失败, userId=${userId}:`, error)
      throw error
    }
  }

  /**
   * 获取状态显示文本
   * @param status 状态值
   * @returns 状态文本
   */
  static getStatusText(status: number): string {
    return status === 1 ? '正常' : '禁用'
  }

  /**
   * 获取角色显示文本
   * @param roleCode 角色编码
   * @returns 角色文本
   */
  static getRoleText(roleCode: string): string {
    const roleMap: Record<string, string> = {
      ADMIN: '管理员',
      MEMBER: '普通会员'
    }
    return roleMap[roleCode] || roleCode
  }

  /**
   * 获取状态标签类型
   * @param status 状态值
   * @returns Element Plus 标签类型
   */
  static getStatusTagType(status: number): string {
    return status === 1 ? 'success' : 'warning'
  }

  /**
   * 获取角色标签类型
   * @param roleCode 角色编码
   * @returns Element Plus 标签类型
   */
  static getRoleTagType(roleCode: string): string {
    return roleCode === 'ADMIN' ? 'danger' : 'success'
  }

  /**
   * 获取操作文本（封禁/解封）
   * @param status 当前状态
   * @returns 操作文本
   */
  static getToggleActionText(status: number): string {
    return status === 1 ? '封禁' : '解封'
  }
}