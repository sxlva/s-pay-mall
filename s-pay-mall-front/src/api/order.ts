/**
 * 商城订单 API 函数
 *
 * @author 傅崇睿
 */

import { mallInstance } from '../utils/axios';
import type { Order, OrderCreateResult, OrderListParams } from '../types/order';

/** 库存检查结果 */
export interface StockCheckResult {
  /** 是否检查成功 */
  success: boolean;
  /** 提示信息 */
  message?: string;
  /** 各商品库存状态：key-商品ID value-{可用库存, 需要数量} */
  stockStatus?: Record<number, { available: number; required: number }>;
}

/**
 * 获取订单列表
 * @param params 查询参数
 * @returns 订单列表
 */
export const getOrderList = (params?: OrderListParams): Promise<Order[]> => {
  console.log('【订单查询参数】', params);
  return mallInstance.get('/orders', { params });
};

/**
 * 检查订单库存
 * @param orderNo 订单号
 * @returns 库存检查结果
 */
export const checkStock = (orderNo: string): Promise<StockCheckResult> => {
  return mallInstance.get(`/orders/${orderNo}/check-stock`);
};

/**
 * 按状态获取订单
 * @param status 订单状态
 * @returns 订单列表
 */
export const getOrdersByStatus = (status: string): Promise<Order[]> => {
  return mallInstance.get('/orders', { params: { status } });
};

/**
 * 创建订单
 * @param address 收货地址
 * @returns 创建结果（可能包含支付表单 HTML）
 */
export const createOrder = (address: string): Promise<OrderCreateResult | string> => {
  return mallInstance.post('/orders', { address });
};

/**
 * 继续支付订单
 * @param orderNo 订单号
 * @returns 支付结果（可能包含支付表单 HTML）
 */
export const continuePay = (orderNo: string): Promise<OrderCreateResult | string> => {
  return mallInstance.get(`/orders/${orderNo}/continue-pay`);
};

/**
 * 获取订单详情
 * @param orderId 订单 ID
 * @returns 订单详情
 */
export const getOrderDetail = (orderId: number): Promise<Order> => {
  return mallInstance.get(`/orders/${orderId}`);
};

/**
 * 取消订单
 * @param orderId 订单 ID
 * @returns 取消结果
 */
export const cancelOrder = (orderId: number): Promise<number> => {
  return mallInstance.delete(`/orders/${orderId}`);
};
