/**
 * 管理员后台-订单管理 API 函数
 *
 * <p>FP0-2：由 api/admin.ts 按业务拆分迁入。</p>
 *
 * @author 傅崇睿
 */

import { adminInstance } from '../../utils/axios'
import type { OrderAdminVO, OrderAdminQueryParams } from '../../types/domain/admin'

/**
 * 获取订单列表
 * @param params 查询参数
 * @returns 订单列表
 */
export const getAdminOrders = (params: OrderAdminQueryParams = {}): Promise<OrderAdminVO[]> => {
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
