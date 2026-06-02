import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { Order, OrderListParams } from '../types/order';
import { getOrderList, createOrder } from '../api/order';

export const useOrderStore = defineStore('order', () => {
  const orders = ref<Order[]>([]);
  const currentOrder = ref<Order | null>(null);
  const loading = ref(false);

  const orderCount = computed(() => orders.value.length);

  const ordersByStatus = computed(() => {
    return (status: string) => orders.value.filter(order => order.status === status);
  });

  const pendingOrders = computed(() => orders.value.filter(order => order.status === 'INIT'));
  const paidOrders = computed(() => orders.value.filter(order => order.status === 'PAID'));
  const shippedOrders = computed(() => orders.value.filter(order => order.status === 'SHIPPED'));
  const completedOrders = computed(() => orders.value.filter(order => order.status === 'DONE'));
  const canceledOrders = computed(() => orders.value.filter(order => order.status === 'CANCELED'));

  const loadOrders = async (params?: OrderListParams): Promise<void> => {
    loading.value = true;
    try {
      orders.value = await getOrderList(params);
    } catch (error) {
      console.error('加载订单列表失败:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const createNewOrder = async (address: string): Promise<string | null> => {
    loading.value = true;
    try {
      const result = await createOrder(address);
      if (typeof result === 'object' && result !== null) {
        const payUrl = result.payUrl || result._html || result.html;
        if (typeof payUrl === 'string' && payUrl.includes('<form')) {
          return payUrl;
        }
      }
      await loadOrders();
      return null;
    } catch (error) {
      console.error('创建订单失败:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const setCurrentOrder = (order: Order | null): void => {
    currentOrder.value = order;
  };

  const clearOrders = (): void => {
    orders.value = [];
    currentOrder.value = null;
  };

  return {
    orders,
    currentOrder,
    loading,
    orderCount,
    ordersByStatus,
    pendingOrders,
    paidOrders,
    shippedOrders,
    completedOrders,
    canceledOrders,
    loadOrders,
    createNewOrder,
    setCurrentOrder,
    clearOrders
  };
});