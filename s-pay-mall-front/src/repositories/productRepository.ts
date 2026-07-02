/**
 * 商品仓储层
 * 职责：封装数据访问逻辑，处理 HTTP 请求，将后端原始数据转换为前端业务对象
 * DDD 分层：Infrastructure Layer（基础设施层）
 *
 * @author 傅崇睿
 */
import type { ProductVO, CategoryVO, ProductQueryParams } from '../types/domain/product'
import { normalizeProduct, normalizeProducts } from '../utils/product'

const API_BASE = '/mall-api/v1'

const getAuthHeaders = (): Record<string, string> => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

/**
 * 商品仓储接口
 * 提供统一的数据访问入口，Store 层只调用此接口，不直接处理 HTTP
 */
export const productRepository = {
  /**
   * 获取分类列表
   * @returns Promise<CategoryVO[]> 分类列表
   * @throws Error 当后端返回非成功状态码时抛出异常
   */
  async fetchCategories(): Promise<CategoryVO[]> {
    const response = await fetch(`${API_BASE}/categories`, { headers: getAuthHeaders() })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const data = await response.json()
    
    if (data.code !== '0000') {
      throw new Error(data.info || '加载分类失败')
    }
    
    return data.data || []
  },

  /**
   * 获取商品列表（支持查询参数）
   * @param params 查询参数
   * @returns Promise<ProductVO[]> 商品列表
   * @throws Error 当后端返回非成功状态码时抛出异常
   */
  async fetchProducts(params: ProductQueryParams): Promise<ProductVO[]> {
    const url = new URL(`${API_BASE}/products`, window.location.origin)

    // 构建查询参数
    if (params.categoryId !== null && params.categoryId !== undefined) {
      url.searchParams.set('categoryId', params.categoryId.toString())
    }

    if (params.keyword) {
      url.searchParams.set('keyword', params.keyword)
    }

    if (params.minPrice !== undefined) {
      url.searchParams.set('minPrice', params.minPrice.toString())
    }

    if (params.maxPrice !== undefined) {
      url.searchParams.set('maxPrice', params.maxPrice.toString())
    }

    if (params.status !== undefined) {
      url.searchParams.set('status', params.status.toString())
    }

    const response = await fetch(url.toString(), { headers: getAuthHeaders() })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const data = await response.json()
    
    if (data.code !== '0000') {
      throw new Error(data.info || '加载商品失败')
    }
    
    return normalizeProducts(data.data || [])
  },

  /**
   * 获取商品详情
   * @param productId 商品 ID
   * @returns Promise<ProductVO> 商品详情
   * @throws Error 当后端返回非成功状态码时抛出异常
   */
  async fetchProduct(productId: number): Promise<ProductVO> {
    const response = await fetch(`${API_BASE}/products/${productId}`, { headers: getAuthHeaders() })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const data = await response.json()
    
    if (data.code !== '0000') {
      throw new Error(data.info || '加载商品详情失败')
    }
    
    return normalizeProduct(data.data)
  }
}