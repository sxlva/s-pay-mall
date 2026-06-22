/**
 * 管理员模块 API 契约类型
 * 定义 API 请求/响应的数据结构
 *
 * @author 傅崇睿
 */

/**
 * 保存用户参数（API 契约）
 */
export interface SaveUserParams {
  /** 用户名 */
  username: string
  /** 邮箱 */
  email?: string
  /** 密码（新建时必填） */
  password?: string
  /** 角色编码 */
  role: string
  /** 状态：1-正常 0-禁用 */
  status?: number
}

/**
 * 保存分类参数（API 契约）
 */
export interface SaveCategoryParams {
  /** 分类名称 */
  name: string
  /** 状态：1-启用 0-禁用 */
  status?: number
}

/**
 * 保存商品参数（API 契约）
 */
export interface SaveProductParams {
  /** 所属分类 ID */
  categoryId: number
  /** 商品名称 */
  name: string
  /** 商品描述 */
  description?: string
  /** 商品价格 */
  price: number
  /** 库存数量 */
  stock: number
  /** 状态：1-上架 0-下架 */
  status?: number
}
