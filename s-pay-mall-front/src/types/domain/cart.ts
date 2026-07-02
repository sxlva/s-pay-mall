/**
 * 购物车类型定义
 *
 * @author 傅崇睿
 */

/**
 * 购物车项 - 前端标准接口
 */
export interface CartItem {
  /** 购物车项 ID */
  id: number
  /** 商品 ID */
  productId: number
  /** 商品名称 */
  productName: string
  /** 商品单价 */
  productPrice: number
  /** 购买数量 */
  quantity: number
  /** 是否选中 */
  selected: boolean
  /** 小计金额（productPrice * quantity） */
  itemAmount: number
  /** 库存数量 */
  stock: number
}

/**
 * 购物车项 - 后端原始响应接口（兼容多种字段命名）
 * 用于接收后端返回的数据，避免使用 any 类型
 */
export interface CartItemRaw extends Record<string, unknown> {
  id?: number | string
  /** 购物车 ID（后端字段名） */
  cartId?: number | string
  /** 购物车项 ID（后端字段名） */
  itemId?: number | string
  productId?: number | string
  productName?: string
  /** 商品名称（后端字段名） */
  title?: string
  price?: number | string
  productPrice?: number | string
  /** 商品价格（后端字段名） */
  skuPrice?: number | string
  quantity?: number | string
  /** 数量（后端字段名） */
  count?: number | string
  /** 数量（后端字段名） */
  num?: number | string
  selected?: boolean
  itemAmount?: number | string
  stock?: number | string
}

/**
 * 购物车状态接口
 */
export interface CartState {
  /** 购物车商品列表 */
  items: CartItem[]
  /** 是否正在加载 */
  loading: boolean
  /** 已选中商品总金额 */
  totalAmount: number
  /** 已选中商品总数量 */
  totalCount: number
  /** 是否全选 */
  allSelected: boolean
}

/**
 * 购物车添加请求参数
 */
export interface CartAddParams {
  /** 商品 ID */
  productId: number
  /** 添加数量（默认 1） */
  quantity?: number
}

/**
 * 购物车更新请求参数
 */
export interface CartUpdateQuantityParams {
  /** 商品 ID */
  productId: number
  /** 更新后的数量 */
  quantity: number
}
