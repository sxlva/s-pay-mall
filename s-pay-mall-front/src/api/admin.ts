import { adminInstance } from '../utils/axios'
import type { UserVO } from '../types/domain/user'
import type { SaveUserParams, SaveCategoryParams, SaveProductParams } from '../types/api/admin'

// ==================== 用户管理 ====================

export type { UserVO } from '../types/domain/user'

export type { SaveUserParams } from '../types/api/admin'

export const getAdminUsers = (params: Record<string, unknown> = {}): Promise<UserVO[]> => {
  return adminInstance.get('/admin/users', { params })
}

export const saveAdminUser = (data: SaveUserParams): Promise<UserVO> => {
  return adminInstance.post('/admin/users', data)
}

export const updateAdminUserStatus = (id: number, status: number): Promise<UserVO> => {
  return adminInstance.put(`/admin/users/${id}/status`, null, { params: { status } })
}

export const deleteAdminUser = (id: number): Promise<void> => {
  return adminInstance.delete(`/admin/users/${id}`)
}

// ==================== 分类管理 ====================

export interface CategoryVO {
  id: number
  name: string
  status: number
  createTime: string
  updateTime: string
}

export type { SaveCategoryParams, SaveProductParams } from '../types/api/admin'

export const getAdminCategories = (): Promise<CategoryVO[]> => {
  return adminInstance.get('/admin/categories')
}

export const saveAdminCategory = (data: SaveCategoryParams): Promise<CategoryVO> => {
  return adminInstance.post('/admin/categories', data)
}

export const deleteAdminCategory = (id: number): Promise<void> => {
  return adminInstance.delete(`/admin/categories/${id}`)
}

// ==================== 商品管理 ====================

export interface ProductAdminVO {
  id: number
  categoryId: number
  categoryName: string
  name: string
  description: string
  price: number
  stock: number
  status: number
  createTime: string
  updateTime: string
}

export const getAdminProducts = (params: Record<string, unknown> = {}): Promise<ProductAdminVO[]> => {
  return adminInstance.get('/admin/products', { params })
}

export const saveAdminProduct = (data: SaveProductParams): Promise<ProductAdminVO> => {
  return adminInstance.post('/admin/products', data)
}

export const deleteAdminProduct = (id: number): Promise<void> => {
  return adminInstance.delete(`/admin/products/${id}`)
}

// ==================== 订单管理 ====================

export interface OrderItemVO {
  id: number
  productId: number
  productName: string
  price: number
  quantity: number
  itemAmount: number
}

export interface OrderAdminVO {
  id: number
  orderNo: string
  userId: number
  totalAmount: number
  address: string
  status: string
  statusDesc: string
  createTime: string
  updateTime: string
  totalCount: number
  items: OrderItemVO[]
}

export interface OrderQueryParams {
  status?: string
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}

export const getAdminOrders = (params: OrderQueryParams = {}): Promise<OrderAdminVO[]> => {
  return adminInstance.get('/admin/orders', { params })
}

export const deliverOrder = (id: number): Promise<OrderAdminVO> => {
  return adminInstance.put(`/admin/orders/${id}/deliver`)
}

export const cancelOrder = (id: number): Promise<OrderAdminVO> => {
  return adminInstance.put(`/admin/orders/${id}/cancel`)
}

export const updateAdminOrderStatus = (id: number, status: string): Promise<OrderAdminVO> => {
  return adminInstance.put(`/admin/orders/${id}/status`, { status })
}

export const deleteAdminOrder = (id: number): Promise<void> => {
  return adminInstance.delete(`/admin/orders/${id}`)
}

// ==================== 统计数据 ====================

export interface SalesTrendVO {
  date: string
  amount: number
  count: number
}

export interface CategoryRatioVO {
  categoryId: number
  categoryName: string
  amount: number
  ratio: number
}

export const getSalesTrend = (): Promise<SalesTrendVO[]> => {
  return adminInstance.get('/admin/statistics/sales-trend')
}

export const getCategoryRatio = (): Promise<CategoryRatioVO[]> => {
  return adminInstance.get('/admin/statistics/category-ratio')
}
