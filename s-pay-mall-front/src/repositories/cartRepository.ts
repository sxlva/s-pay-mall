/**
 * 购物车仓储层
 * 职责：封装数据访问逻辑，处理 HTTP 请求，将后端原始数据转换为前端业务对象
 * DDD 分层：Infrastructure Layer（基础设施层）
 */
import type { CartItem, CartItemRaw, CartAddParams, CartUpdateQuantityParams } from '../types/cart'

const API_BASE = '/mall-api/v1'

const getAuthHeaders = (): Record<string, string> => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

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
 * 购物车仓储接口
 * 提供统一的数据访问入口，Store 层只调用此接口，不直接处理 HTTP
 */
export const cartRepository = {
  /**
   * 获取购物车列表
   * @returns Promise<CartItem[]> 转换后的购物车项数组
   * @throws Error 当后端返回非成功状态码时抛出异常
   */
  async fetchItems(): Promise<CartItem[]> {
    const response = await fetch(`${API_BASE}/cart`, { headers: getAuthHeaders() })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const data = await response.json()
    
    if (data.code !== '0000') {
      throw new Error(data.info || '加载购物车失败')
    }
    
    console.log('【购物车原始数据】', data.data)
    const items = (data.data || []).map(mapRawToCartItem)
    console.log('【购物车清洗后数据】', items)
    
    return items
  },

  /**
   * 添加商品到购物车
   * @param params 添加参数
   * @throws Error 当后端返回非成功状态码时抛出异常
   */
  async add(params: CartAddParams): Promise<void> {
    const response = await fetch(`${API_BASE}/cart`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify(params)
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const data = await response.json()
    
    if (data.code !== '0000') {
      throw new Error(data.info || '添加失败')
    }
  },

  /**
   * 更新购物车商品数量
   * @param params 更新参数
   * @throws Error 当后端返回非成功状态码时抛出异常
   */
  async updateQuantity(params: CartUpdateQuantityParams): Promise<void> {
    const response = await fetch(`${API_BASE}/cart/quantity`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify(params)
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const data = await response.json()
    
    if (data.code !== '0000') {
      throw new Error(data.info || '更新失败')
    }
  },

  /**
   * 删除购物车项
   * @param cartItemId 购物车项 ID
   * @throws Error 当后端返回非成功状态码时抛出异常
   */
  async delete(cartItemId: number): Promise<void> {
    const response = await fetch(`${API_BASE}/cart/delete?itemId=${cartItemId}`, {
      method: 'DELETE',
      headers: getAuthHeaders()
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const data = await response.json()
    
    if (data.code !== '0000') {
      throw new Error(data.info || '删除失败')
    }
  },

  /**
   * 清空购物车
   * @throws Error 当后端返回非成功状态码时抛出异常
   */
  async clear(): Promise<void> {
    const response = await fetch(`${API_BASE}/cart`, {
      method: 'DELETE',
      headers: getAuthHeaders()
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const data = await response.json()
    
    if (data.code !== '0000') {
      throw new Error(data.info || '清空失败')
    }
  }
}