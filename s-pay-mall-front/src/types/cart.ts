/**
 * 购物车项 - 前端标准接口
 */
export interface CartItem {
  id: number
  productId: number
  productName: string
  productPrice: number
  quantity: number
  selected: boolean
  itemAmount: number
  stock: number
}

/**
 * 购物车项 - 后端原始响应接口（兼容多种字段命名）
 * 用于接收后端返回的数据，避免使用 any 类型
 */
export interface CartItemRaw extends Record<string, unknown> {
  id?: number | string
  cartId?: number | string
  itemId?: number | string
  productId?: number | string
  productName?: string
  title?: string
  price?: number | string
  productPrice?: number | string
  skuPrice?: number | string
  quantity?: number | string
  count?: number | string
  num?: number | string
  selected?: boolean
  itemAmount?: number | string
  stock?: number | string
}

/**
 * 购物车状态接口
 */
export interface CartState {
  items: CartItem[]
  loading: boolean
  totalAmount: number
  totalCount: number
  allSelected: boolean
}

/**
 * 购物车添加请求参数
 */
export interface CartAddParams {
  productId: number
  quantity?: number
}

/**
 * 购物车更新请求参数
 */
export interface CartUpdateQuantityParams {
  productId: number
  quantity: number
}
