/**
 * 支付状态管理 Store
 * 职责：支付轮询、倒计时、状态管理
 * DDD 分层：Application/Domain Layer（应用层/领域层）
 *
 * @author 傅崇睿
 */

import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { PayOrder, PollingState, PayResult } from '../types/payment';
import { orderRepository } from '../repositories/orderRepository';
import type { Order } from '../types/order';

/** 轮询间隔（毫秒） */
const POLLING_INTERVAL = 3000;
/** 最大轮询次数（3秒 x 60次 = 3分钟） */
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

  /**
   * 开始轮询支付状态
   * @param orderNo 订单号
   * @param initialCountdown 初始倒计时秒数（默认 180）
   */
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

  /** 停止轮询并清理定时器 */
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

  /**
   * 检查支付状态（轮询回调）
   * @param orderNo 订单号
   */
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

      const orders = await orderRepository.getOrderList();
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

  /** 处理支付成功 */
  const handlePaymentSuccess = () => {
    stopPolling();
    pollingState.value = 'success';
    console.log('支付成功');
  };

  /** 处理支付超时 */
  const handleTimeout = () => {
    stopPolling();
    pollingState.value = 'timeout';
    error.value = '支付超时，请重新下单';
  };

  /**
   * 处理支付失败
   * @param errorMessage 错误信息
   */
  const handlePaymentFailed = (errorMessage: string) => {
    stopPolling();
    pollingState.value = 'failed';
    error.value = errorMessage;
  };

  /**
   * 设置支付链接
   * @param url 支付链接或 HTML 表单
   */
  const setPayUrl = (url: string | null) => {
    payUrl.value = url;
  };

  /**
   * 初始化支付订单
   * @param result 支付结果
   */
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

  /**
   * 重定向到支付页面
   * @param payUrl 支付 URL 或表单 HTML
   */
  const redirectToPay = (payUrl: string) => {
    // 如果是表单 HTML，动态创建并提交
    if (payUrl.includes('<form')) {
      const tempContainer = document.createElement('div');
      tempContainer.innerHTML = payUrl;
      const form = tempContainer.querySelector('form');
      if (form) {
        document.body.appendChild(form);
        form.submit();
      }
    } else {
      // 如果是普通 URL，直接跳转
      window.location.href = payUrl;
    }
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
    resetPayment,
    redirectToPay
  };
});