/**
 * 商城订单 API 函数
 *
 * <p>FP0-1：repositories/orderRepository 并入本文件，订单数据出口唯一；
 * FP0-4：后端创建/继续支付响应的订单号字段为 orderId（UserOrderCreateRes），
 * 在本层统一映射为前端 OrderCreateResult.orderNo，调用方不再感知后端字段名。</p>
 *
 * @author 傅崇睿
 */

import { mallInstance } from '../utils/axios';
import type { Order, OrderCreateResult, OrderListParams } from '../types/domain/order';

/** 后端创建/继续支付响应原始结构（UserOrderCreateRes） */
interface OrderCreateRaw {
  /** 订单编号（后端字段名） */
  orderId?: string;
  /** 订单编号（兼容驼峰） */
  orderNo?: string;
  /** 订单总金额 */
  totalAmount: number;
  /** 订单状态 */
  status?: string;
  /** 支付链接（可能为 HTML 表单） */
  payUrl?: string | null;
  /** 支付表单 HTML */
  html?: string;
}

/** 库存检查结果（FP1-3：已删除后端不存在的 stockStatus 字段） */
export interface StockCheckResult {
  /** 是否检查成功 */
  success: boolean;
  /** 提示信息 */
  message?: string;
}

/**
 * 将后端创建订单响应映射为前端 OrderCreateResult
 * FP0-4：orderId → orderNo，调用方拿到的 orderNo 始终真实有效
 */
const toCreateResult = (raw: OrderCreateRaw): OrderCreateResult => ({
  orderNo: raw.orderNo ?? raw.orderId ?? '',
  totalAmount: raw.totalAmount,
  status: raw.status ?? '',
  payUrl: raw.payUrl ?? null,
  html: raw.html
});

/**
 * 获取订单列表
 * @param params 查询参数
 * @returns 订单列表
 */
export const getOrderList = (params?: OrderListParams): Promise<Order[]> => {
  return mallInstance.get('/orders', { params });
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
 * 检查订单库存
 * @param orderNo 订单号
 * @returns 库存检查结果
 */
export const checkStock = (orderNo: string): Promise<StockCheckResult> => {
  return mallInstance.get(`/orders/${orderNo}/check-stock`);
};

/**
 * 创建订单
 * @param address 收货地址
 * @returns 创建结果（含真实 orderNo 与可能的支付表单 HTML）
 */
export const createOrder = async (address: string): Promise<OrderCreateResult> => {
  const raw = await mallInstance.post('/orders', { address }) as OrderCreateRaw;
  return toCreateResult(raw);
};

/**
 * 继续支付订单
 * @param orderNo 订单号
 * @returns 支付结果（含真实 orderNo 与可能的支付表单 HTML）
 */
export const continuePay = async (orderNo: string): Promise<OrderCreateResult> => {
  const raw = await mallInstance.get(`/orders/${orderNo}/continue-pay`) as OrderCreateRaw;
  return toCreateResult(raw);
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
