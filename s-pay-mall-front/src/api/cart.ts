/**
 * 购物车 API 函数
 *
 * <p>FP0-1/FP0-3：由 repositories/cartRepository 迁入，统一走 Axios 实例
 * （token 附加、响应解包、错误提示均由拦截器统一处理），不再裸用 fetch。</p>
 *
 * @author 傅崇睿
 */

import { mallInstance } from '../utils/axios'
import type { CartItem, CartItemRaw, CartAddParams, CartUpdateQuantityParams } from '../types/domain/cart'

/**
 * 数据清洗适配器：将后端原始响应转换为前端标准 CartItem 对象
 * 处理字段命名不一致、类型转换、默认值填充等防御性逻辑
 */
const mapRawToCartItem = (item: CartItemRaw): CartItem => {
  const safePrice = Number(item.price || item.productPrice || item.skuPrice || 0)
  const safeQuantity = Number(item.quantity || item.count || item.num || 0)

  return {
    id: Number(item.id || item.cartId || item.itemId || 0),
    productId: Number(item.productId || 0),
    productName: String(item.productName || item.title || '未知商品'),
    productPrice: safePrice,
    quantity: safeQuantity,
    selected: item.selected === undefined ? false : Boolean(item.selected),
    itemAmount: Number(item.itemAmount || (safePrice * safeQuantity) || 0),
    stock: Number(item.stock || 0)
  }
}

/**
 * 获取购物车列表
 * @returns 转换后的购物车项数组
 */
export const fetchCartItems = async (): Promise<CartItem[]> => {
  const data = await mallInstance.get('/cart') as CartItemRaw[]
  return (data || []).map(mapRawToCartItem)
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
