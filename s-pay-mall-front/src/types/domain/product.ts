/**
 * 商品类型定义
 *
 * @author 傅崇睿
 */

/** 商品值对象 */
export interface ProductVO {
  /** 商品 ID */
  id: number
  /** 分类 ID（后端下划线字段） */
  category_id: number
  /** 分类名称（后端下划线字段） */
  category_name: string
  /** 商品名称 */
  name: string
  /** 商品描述 */
  description: string
  /** 商品价格 */
  price: number
  /** 库存数量 */
  stock: number
  /** 状态：1-上架 0-下架 */
  status: number
  /** 创建时间（后端下划线字段） */
  create_time: string
  /** 更新时间（后端下划线字段） */
  update_time: string
}

/** 分类值对象 */
export interface CategoryVO {
  /** 分类 ID */
  id: number
  /** 分类名称 */
  name: string
  /** 状态：1-启用 0-禁用 */
  status: number
  /** 创建时间（后端下划线字段） */
  create_time: string
  /** 更新时间（后端下划线字段） */
  update_time: string
}

/** 商品查询参数 */
export interface ProductQueryParams {
  /** 按分类 ID 过滤 */
  categoryId?: number | null
  /** 按关键词搜索 */
  keyword?: string
  /** 最低价格 */
  minPrice?: number
  /** 最高价格 */
  maxPrice?: number
  /** 按状态过滤 */
  status?: number
}