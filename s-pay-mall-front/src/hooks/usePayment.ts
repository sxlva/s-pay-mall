/**
 * usePayment Hook
 * 
 * 职责：纯粹的支付业务逻辑处理
 * - 管理支付状态（轮询、倒计时、错误处理）
 * - 初始化支付订单
 * - 重置支付状态
 * 
 * 架构原则：
 * - 不直接操作 DOM（由 PaymentSubmitter 组件负责）
 * - 不包含 UI 渲染逻辑
 * - 只关注业务状态管理
 */
import { usePaymentStore } from '../stores/payment'
import { storeToRefs } from 'pinia'
import { onUnmounted } from 'vue'

export function usePayment() {
  const paymentStore = usePaymentStore()

  const {
    currentPayOrder,
    payUrl,
    pollingState,
    pollingCount,
    countdown,
    error,
    isPolling,
    isPaid,
    isFailed
  } = storeToRefs(paymentStore)

  const startPolling = paymentStore.startPolling
  const stopPolling = paymentStore.stopPolling
  const handlePaymentSuccess = paymentStore.handlePaymentSuccess
  const handlePaymentFailed = paymentStore.handlePaymentFailed
  const setPayUrl = paymentStore.setPayUrl
  const initPayOrder = paymentStore.initPayOrder
  const resetPayment = paymentStore.resetPayment
  const redirectToPay = paymentStore.redirectToPay

  /**
   * 清理函数
   * - 停止轮询
   * - 释放资源
   */
  const cleanup = () => {
    stopPolling()
  }

  /**
   * 验证支付 URL 是否有效
   * @param payUrl 支付 URL 或表单 HTML
   * @returns 是否有效
   */
  const validatePayUrl = (payUrl: string): boolean => {
    if (!payUrl || payUrl.trim() === '') {
      return false
    }
    return true
  }

  /**
   * 提取支付表单 HTML
   * 如果 payUrl 已经是表单 HTML，直接返回
   * 如果是 URL 链接，返回 null（需要组件处理）
   */
  const extractPaymentForm = (payUrl: string): string | null => {
    if (!validatePayUrl(payUrl)) {
      handlePaymentFailed('支付链接为空')
      return null
    }

    // 如果包含<form>标签，说明已经是表单 HTML
    if (payUrl.includes('<form')) {
      return payUrl
    }

    // 否则是普通 URL，返回 null
    return null
  }

  // 组件卸载时清理资源
  onUnmounted(() => {
    cleanup()
  })

  return {
    // 状态
    currentPayOrder,
    payUrl,
    pollingState,
    pollingCount,
    countdown,
    error,
    isPolling,
    isPaid,
    isFailed,
    
    // 方法
    startPolling,
    stopPolling,
    handlePaymentSuccess,
    handlePaymentFailed,
    setPayUrl,
    initPayOrder,
    resetPayment,
    redirectToPay,
    cleanup,
    validatePayUrl,
    extractPaymentForm
  }
}