/**
 * 订单仓储层
 * 职责：封装数据访问逻辑，处理 HTTP 请求，将后端原始数据转换为前端业务对象
 * DDD 分层：Infrastructure Layer（基础设施层）
 */
import { mallInstance } from '@/utils/axios'
import type { Order, OrderCreateResult, OrderListParams } from '@/types/order'

/**
 * 订单仓储接口
 * 提供统一的数据访问入口，Store 层只调用此接口，不直接处理 HTTP
 */
export const orderRepository = {
  /**
   * 获取订单列表（兼容 API 层的 getOrderList 方法名）
   * @param params 查询参数
   * @returns Promise<Order[]> 订单列表
   * @throws Error 当请求失败时抛出异常
   */
  async getOrderList(params?: OrderListParams): Promise<Order[]> {
    console.log('【订单查询参数】', params)
    const response = await mallInstance.get('/orders', { params })
    return response as unknown as Order[]
  },

  /**
   * 获取订单列表
   * @param params 查询参数
   * @returns Promise<Order[]> 订单列表
   * @throws Error 当请求失败时抛出异常
   */
  async fetchOrders(params?: OrderListParams): Promise<Order[]> {
    return this.getOrderList(params)
  },

  /**
   * 根据状态获取订单列表
   * @param status 订单状态
   * @returns Promise<Order[]> 订单列表
   * @throws Error 当请求失败时抛出异常
   */
  async fetchOrdersByStatus(status: string): Promise<Order[]> {
    const response = await mallInstance.get('/orders', { params: { status } })
    return response as unknown as Order[]
  },

  /**
   * 创建订单
   * @param address 收货地址
   * @returns Promise<OrderCreateResult> 创建结果（包含支付 URL）
   * @throws Error 当请求失败时抛出异常
   */
  async create(address: string): Promise<OrderCreateResult> {
    const response = await mallInstance.post('/orders', { address })
    return response as unknown as OrderCreateResult
  },

  /**
   * 继续支付订单
   * @param orderNo 订单号
   * @returns Promise<OrderCreateResult> 支付结果（包含支付表单 HTML）
   * @throws Error 当请求失败时抛出异常
   */
  async continuePay(orderNo: string): Promise<OrderCreateResult> {
    const response = await mallInstance.get(`/orders/${orderNo}/continue-pay`)
    return response as unknown as OrderCreateResult
  },

  /**
   * 获取订单详情
   * @param orderId 订单 ID
   * @returns Promise<Order> 订单详情
   * @throws Error 当请求失败时抛出异常
   */
  async fetchDetail(orderId: number): Promise<Order> {
    const response = await mallInstance.get(`/orders/${orderId}`)
    return response as unknown as Order
  },

  /**
   * 取消订单
   * @param orderId 订单 ID
   * @returns Promise<number> 取消结果
   * @throws Error 当请求失败时抛出异常
   */
  async cancel(orderId: number): Promise<number> {
    const response = await mallInstance.delete(`/orders/${orderId}`)
    return response as unknown as number
  }
}