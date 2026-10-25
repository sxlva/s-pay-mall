/**
 * 购物车 API 函数
 *
 * <p>FP0-1/FP0-3：由 repositories/cartRepository 迁入，统一走 Axios 实例
 * （token 附加、响应解包、错误提示均由拦截器统一处理），不再裸用 fetch。</p>
 *
 * @author 傅崇睿
 */

import { mallInstance } from '../utils/axios'
import type { CartItem, CartAddParams, CartUpdateQuantityParams } from '../types/domain/cart'

/**
 * 获取购物车列表
 * 后端 UserCartItemRes 与 CartItem 字段已一一对齐（API_CONTRACT，2026-10-19），无需映射适配
 * @returns 购物车项数组
 */
export const fetchCartItems = async (): Promise<CartItem[]> => {
  const data = await mallInstance.get('/cart') as CartItem[]
  return data || []
}

/**
 * 添加商品到购物车
 * @param params 添加参数
 */
export const addCartItem = (params: CartAddParams): Promise<unknown> => {
  return mallInstance.post('/cart', params)
}

/**
 * 更新购物车商品数量
 * @param params 更新参数（商品 ID + 更新后的数量）
 */
export const updateCartItemQuantity = (params: CartUpdateQuantityParams): Promise<unknown> => {
  return mallInstance.put('/cart/quantity', params)
}

/**
 * 删除购物车项
 * @param cartItemId 购物车项 ID
 */
export const deleteCartItem = (cartItemId: number): Promise<unknown> => {
  return mallInstance.delete('/cart/delete', { params: { itemId: cartItemId } })
}

/**
 * 清空购物车
 */
export const clearCartItems = (): Promise<unknown> => {
  return mallInstance.delete('/cart')
}
