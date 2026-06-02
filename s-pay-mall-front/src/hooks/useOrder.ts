import { useOrderStore } from '../stores/order';
import { storeToRefs } from 'pinia';
import type { OrderListParams } from '../types/order';

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