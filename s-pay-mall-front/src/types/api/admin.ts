/**
 * 管理员模块 API 契约类型
 * 定义 API 请求/响应的数据结构
 */

/**
 * 保存用户参数（API 契约）
 */
export interface SaveUserParams {
  username: string
  email?: string
  password?: string
  role: string
  status?: number
}

/**
 * 保存分类参数（API 契约）
 */
export interface SaveCategoryParams {
  name: string
  status?: number
}

/**
 * 保存商品参数（API 契约）
 */
export interface SaveProductParams {
  categoryId: number
  name: string
  description?: string
  price: number
  stock: number
  status?: number
}
