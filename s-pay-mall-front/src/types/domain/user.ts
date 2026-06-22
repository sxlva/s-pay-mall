/**
 * 用户领域类型
 * 核心业务实体定义，与具体技术框架无关
 *
 * @author 傅崇睿
 */

/**
 * 用户值对象
 * 管理员后台用户展示实体
 */
export interface UserVO {
  /** 用户 ID */
  id: number
  /** 用户名 */
  username: string
  /** 邮箱 */
  email: string
  /** 角色名称 */
  role: string
  /** 角色编码：ADMIN-管理员 MEMBER-普通会员 VIP-VIP会员 GUEST-普通用户 */
  roleCode: string
  /** 状态：1-正常 0-禁用 */
  status: number
  /** 创建时间（驼峰字段） */
  createTime: string
  /** 创建时间（下划线字段） */
  create_time: string
  /** 更新时间（驼峰字段） */
  updateTime: string
  /** 更新时间（下划线字段） */
  update_time: string
}

/**
 * 用户查询参数
 */
export interface UserQueryParams {
  /** 按用户名搜索 */
  username?: string
  /** 按状态过滤：1-正常 0-禁用 */
  status?: number
  /** 按角色编码过滤 */
  roleCode?: string
}
