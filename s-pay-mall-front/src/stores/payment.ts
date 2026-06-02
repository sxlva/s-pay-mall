import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { PayOrder, PollingState, PayResult } from '../types/payment';
import { getOrderList } from '../api/order';
import type { Order } from '../types/order';

const POLLING_INTERVAL = 3000;
const MAX_POLLING_COUNT = 60;

export const usePaymentStore = defineStore('payment', () => {
  const currentPayOrder = ref<PayOrder | null>(null);
  const payUrl = ref<string | null>(null);
  const pollingState = ref<PollingState>('idle');
  const pollingCount = ref(0);
  const countdown = ref(0);
  const error = ref<string | null>(null);

  let pollingTimer: ReturnType<typeof setInterval> | null = null;
  let countdownTimer: ReturnType<typeof setInterval> | null = null;

  const isPolling = computed(() => pollingState.value === 'polling');
  const isPaid = computed(() => pollingState.value === 'success');
  const isFailed = computed(() => pollingState.value === 'failed' || pollingState.value === 'timeout');

  const startPolling = (orderNo: string, initialCountdown: number = 180) => {
    if (pollingTimer) {
      stopPolling();
    }

    pollingState.value = 'polling';
    pollingCount.value = 0;
    countdown.value = initialCountdown;
    error.value = null;

    countdownTimer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) {
        handleTimeout();
      }
    }, 1000);

    pollingTimer = setInterval(async () => {
      await checkPaymentStatus(orderNo);
    }, POLLING_INTERVAL);
  };

  const stopPolling = () => {
    if (pollingTimer) {
      clearInterval(pollingTimer);
      pollingTimer = null;
    }
    if (countdownTimer) {
      clearInterval(countdownTimer);
      countdownTimer = null;
    }
    pollingState.value = 'idle';
    pollingCount.value = 0;
  };

  const checkPaymentStatus = async (orderNo: string) => {
    if (pollingState.value !== 'polling') {
      return;
    }

    try {
      pollingCount.value++;

      if (pollingCount.value >= MAX_POLLING_COUNT) {
        handleTimeout();
        return;
      }

      const orders = await getOrderList();
      const paidOrder = orders.find(
        (o: Order) => o.orderNo === orderNo && o.status === 'PAID'
      );

      if (paidOrder) {
        handlePaymentSuccess();
      }
    } catch (err) {
      console.error('轮询检查支付状态失败:', err);
    }
  };

  const handlePaymentSuccess = () => {
    stopPolling();
    pollingState.value = 'success';
    console.log('支付成功');
  };

  const handleTimeout = () => {
    stopPolling();
    pollingState.value = 'timeout';
    error.value = '支付超时，请重新下单';
  };

  const handlePaymentFailed = (errorMessage: string) => {
    stopPolling();
    pollingState.value = 'failed';
    error.value = errorMessage;
  };

  const setPayUrl = (url: string | null) => {
    payUrl.value = url;
  };

  const initPayOrder = (result: PayResult) => {
    currentPayOrder.value = {
      orderNo: result.orderNo,
      userId: '',
      productId: '',
      productName: '',
      totalAmount: result.totalAmount,
      status: 'PAYING',
      statusDesc: '支付中',
      payUrl: result.payUrl,
      tradeNo: null,
      payTime: null,
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    };
    payUrl.value = result.payUrl;
  };

  const resetPayment = () => {
    stopPolling();
    currentPayOrder.value = null;
    payUrl.value = null;
    error.value = null;
    pollingState.value = 'idle';
  };

  return {
    currentPayOrder,
    payUrl,
    pollingState,
    pollingCount,
    countdown,
    error,
    isPolling,
    isPaid,
    isFailed,
    startPolling,
    stopPolling,
    handlePaymentSuccess,
    handlePaymentFailed,
    setPayUrl,
    initPayOrder,
    resetPayment
  };
});