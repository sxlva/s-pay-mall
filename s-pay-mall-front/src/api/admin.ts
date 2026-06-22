/**
 * 管理员后台 API 函数
 *
 * @author 傅崇睿
 */

import { adminInstance } from '../utils/axios'
import type { UserVO, UserQueryParams } from '../types/domain/user'
import type { SaveUserParams, SaveCategoryParams, SaveProductParams } from '../types/api/admin'

// ==================== 用户管理 ====================

export type { UserVO } from '../types/domain/user'

export type { SaveUserParams } from '../types/api/admin'

/**
 * 获取管理员用户列表
 * @param params 查询参数
 * @returns 用户列表
 */
export const getAdminUsers = (params: UserQueryParams = {}): Promise<UserVO[]> => {
  return adminInstance.get('/admin/users', { params })
}

/**
 * 创建或更新管理员用户
 * @param data 用户数据
 * @returns 保存后的用户数据
 */
export const saveAdminUser = (data: SaveUserParams): Promise<UserVO> => {
  return adminInstance.post('/admin/users', data)
}

/**
 * 更新用户状态（封禁/解封）
 * @param id 用户 ID
 * @param status 新状态值：1-正常 0-禁用
 * @returns 更新后的用户数据
 */
export const updateAdminUserStatus = (id: number, status: number): Promise<UserVO> => {
  return adminInstance.put(`/admin/users/${id}/status`, null, { params: { status } })
}

/**
 * 删除管理员用户
 * @param id 用户 ID
 * @returns void
 */
export const deleteAdminUser = (id: number): Promise<void> => {
  return adminInstance.delete(`/admin/users/${id}`)
}

// ==================== 分类管理 ====================

/** 分类值对象（后台管理） */
export interface CategoryVO {
  /** 分类 ID */
  id: number
  /** 分类名称 */
  name: string
  /** 状态：1-启用 0-禁用 */
  status: number
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

export type { SaveCategoryParams, SaveProductParams } from '../types/api/admin'

/**
 * 获取分类列表
 * @returns 分类列表
 */
export const getAdminCategories = (): Promise<CategoryVO[]> => {
  return adminInstance.get('/admin/categories')
}

/**
 * 创建或更新分类
 * @param data 分类数据
 * @returns 保存后的分类数据
 */
export const saveAdminCategory = (data: SaveCategoryParams): Promise<CategoryVO> => {
  return adminInstance.post('/admin/categories', data)
}

/**
 * 删除分类
 * @param id 分类 ID
 * @returns void
 */
export const deleteAdminCategory = (id: number): Promise<void> => {
  return adminInstance.delete(`/admin/categories/${id}`)
}

// ==================== 商品管理 ====================

/** 商品值对象（后台管理） */
export interface ProductAdminVO {
  /** 商品 ID */
  id: number
  /** 所属分类 ID */
  categoryId: number
  /** 分类名称 */
  categoryName: string
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
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

/**
 * 获取商品列表
 * @param params 查询参数
 * @returns 商品列表
 */
export const getAdminProducts = (params: Record<string, unknown> = {}): Promise<ProductAdminVO[]> => {
  return adminInstance.get('/admin/products', { params })
}

/**
 * 创建或更新商品
 * @param data 商品数据
 * @returns 保存后的商品数据
 */
export const saveAdminProduct = (data: SaveProductParams): Promise<ProductAdminVO> => {
  return adminInstance.post('/admin/products', data)
}

/**
 * 删除商品
 * @param id 商品 ID
 * @returns void
 */
export const deleteAdminProduct = (id: number): Promise<void> => {
  return adminInstance.delete(`/admin/products/${id}`)
}

// ==================== 订单管理 ====================

/** 订单商品项（后台管理） */
export interface OrderItemVO {
  /** 订单项 ID */
  id: number
  /** 商品 ID */
  productId: number
  /** 商品名称 */
  productName: string
  /** 商品单价 */
  price: number
  /** 购买数量 */
  quantity: number
  /** 小计金额 */
  itemAmount: number
}

/** 订单值对象（后台管理） */
export interface OrderAdminVO {
  /** 订单 ID */
  id: number
  /** 订单编号 */
  orderNo: string
  /** 用户 ID */
  userId: number
  /** 订单总金额 */
  totalAmount: number
  /** 收货地址 */
  address: string
  /** 订单状态 */
  status: string
  /** 订单状态描述 */
  statusDesc: string
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
  /** 商品总数量 */
  totalCount: number
  /** 订单商品列表 */
  items: OrderItemVO[]
}

/** 订单查询参数（后台管理） */
export interface OrderQueryParams {
  /** 按状态过滤 */
  status?: string
  /** 开始时间 */
  startTime?: string
  /** 结束时间 */
  endTime?: string
  /** 分页页码 */
  page?: number
  /** 每页大小 */
  size?: number
}

/**
 * 获取订单列表
 * @param params 查询参数
 * @returns 订单列表
 */
export const getAdminOrders = (params: OrderQueryParams = {}): Promise<OrderAdminVO[]> => {
  return adminInstance.get('/admin/orders', { params })
}

/**
 * 发货
 * @param id 订单 ID
 * @returns 更新后的订单数据
 */
export const deliverOrder = (id: number): Promise<OrderAdminVO> => {
  return adminInstance.put(`/admin/orders/${id}/deliver`)
}

/**
 * 取消订单
 * @param id 订单 ID
 * @returns 更新后的订单数据
 */
export const cancelOrder = (id: number): Promise<OrderAdminVO> => {
  return adminInstance.put(`/admin/orders/${id}/cancel`)
}

/**
 * 更新订单状态
 * @param id 订单 ID
 * @param status 新状态
 * @returns 更新后的订单数据
 */
export const updateAdminOrderStatus = (id: number, status: string): Promise<OrderAdminVO> => {
  return adminInstance.put(`/admin/orders/${id}/status`, { status })
}

/**
 * 删除订单
 * @param id 订单 ID
 * @returns void
 */
export const deleteAdminOrder = (id: number): Promise<void> => {
  return adminInstance.delete(`/admin/orders/${id}`)
}

// ==================== 统计数据 ====================

/** 销售额走势数据 */
export interface SalesTrendVO {
  /** 日期 */
  date: string
  /** 销售额 */
  amount: number
  /** 订单数 */
  count: number
}

/** 分类销售占比数据 */
export interface CategoryRatioVO {
  /** 分类 ID */
  categoryId: number
  /** 分类名称 */
  categoryName: string
  /** 销售额 */
  amount: number
  /** 占比 */
  ratio: number
}

/**
 * 获取销售额走势
 * @returns 走势数据列表
 */
export const getSalesTrend = (): Promise<SalesTrendVO[]> => {
  return adminInstance.get('/admin/statistics/sales-trend')
}

/**
 * 获取分类销售占比
 * @returns 占比数据列表
 */
export const getCategoryRatio = (): Promise<CategoryRatioVO[]> => {
  return adminInstance.get('/admin/statistics/category-ratio')
}
