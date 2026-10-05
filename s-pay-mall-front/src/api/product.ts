/**
 * 商品 API 函数
 *
 * <p>FP0-1/FP0-3：由 repositories/productRepository 迁入，统一走 Axios 实例，
 * 不再裸用 fetch；查询参数由 Axios 统一序列化。</p>
 *
 * @author 傅崇睿
 */

import { mallInstance } from '../utils/axios'
import type { ProductVO, CategoryVO, ProductQueryParams } from '../types/domain/product'
import { normalizeProduct, normalizeProducts } from '../utils/product'

/**
 * 获取分类列表
 * @returns 分类列表
 */
export const fetchCategories = (): Promise<CategoryVO[]> => {
  return mallInstance.get('/categories')
}

/**
 * 获取商品列表（支持查询参数）
 * @param params 查询参数
 * @returns 规范化后的商品列表
 */
export const fetchProducts = async (params: ProductQueryParams): Promise<ProductVO[]> => {
  const data = await mallInstance.get('/products', { params }) as ProductVO[]
  return normalizeProducts(data || [])
}

/**
 * 获取商品详情
 * @param productId 商品 ID
 * @returns 规范化后的商品详情
 */
export const fetchProduct = async (productId: number): Promise<ProductVO> => {
  const data = await mallInstance.get(`/products/${productId}`) as ProductVO
  return normalizeProduct(data)
}
