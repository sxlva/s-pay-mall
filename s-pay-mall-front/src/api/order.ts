import { mallInstance } from '../utils/axios';
import type { Order, OrderCreateResult, OrderListParams } from '../types/order';

export const getOrderList = (params?: OrderListParams): Promise<Order[]> => {
  console.log('【订单查询参数】', params);
  return mallInstance.get('/orders', { params });
};

export const getOrdersByStatus = (status: string): Promise<Order[]> => {
  return mallInstance.get('/orders', { params: { status } });
};

export const createOrder = (address: string): Promise<OrderCreateResult | string> => {
  return mallInstance.post('/orders', { address });
};

export const continuePay = (orderNo: string): Promise<OrderCreateResult | string> => {
  return mallInstance.get(`/orders/${orderNo}/continue-pay`);
};

export const getOrderDetail = (orderId: number): Promise<Order> => {
  return mallInstance.get(`/orders/${orderId}`);
};

export const cancelOrder = (orderId: number): Promise<number> => {
  return mallInstance.delete(`/orders/${orderId}`);
};
