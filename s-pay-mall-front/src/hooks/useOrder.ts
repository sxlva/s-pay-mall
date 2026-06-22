/**
 * 订单 Hook：封装 orderStore 的响应式数据和方法
 *
 * @author 傅崇睿
 */

import { useOrderStore } from '../stores/order';
import { storeToRefs } from 'pinia';

/**
 * 订单组合式函数
 * @returns 订单响应式状态和方法
 */
export function useOrder() {
  const orderStore = useOrderStore();

  const {
    orders,
    currentOrder,
    loading,
    orderCount,
    pendingOrders,
    paidOrders,
    shippedOrders,
    completedOrders,
    canceledOrders
  } = storeToRefs(orderStore);

  const loadOrders = orderStore.loadOrders;
  const createNewOrder = orderStore.createNewOrder;
  const setCurrentOrder = orderStore.setCurrentOrder;
  const clearOrders = orderStore.clearOrders;
  const ordersByStatus = orderStore.ordersByStatus;

  return {
    orders,
    currentOrder,
    loading,
    orderCount,
    pendingOrders,
    paidOrders,
    shippedOrders,
    completedOrders,
    canceledOrders,
    ordersByStatus,
    loadOrders,
    createNewOrder,
    setCurrentOrder,
    clearOrders
  };
}