/**
 * 用户领域类型
 * 核心业务实体定义，与具体技术框架无关
 */

/**
 * 用户值对象
 * 管理员后台用户展示实体
 */
export interface UserVO {
  id: number
  username: string
  email: string
  role: string
  roleCode: string
  status: number
  createTime: string
  create_time: string
  updateTime: string
  update_time: string
}

/**
 * 用户查询参数
 */
export interface UserQueryParams {
  username?: string
  status?: number
  roleCode?: string
}
