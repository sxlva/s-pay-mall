import { usePaymentStore } from '../stores/payment';
import { storeToRefs } from 'pinia';
import { onUnmounted } from 'vue';

export function usePayment() {
  const paymentStore = usePaymentStore();

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
  } = storeToRefs(paymentStore);

  const startPolling = paymentStore.startPolling;
  const stopPolling = paymentStore.stopPolling;
  const handlePaymentSuccess = paymentStore.handlePaymentSuccess;
  const handlePaymentFailed = paymentStore.handlePaymentFailed;
  const setPayUrl = paymentStore.setPayUrl;
  const initPayOrder = paymentStore.initPayOrder;
  const resetPayment = paymentStore.resetPayment;

  const cleanup = () => {
    stopPolling();
  };

  onUnmounted(() => {
    cleanup();
  });

  const redirectToPay = (payUrl: string) => {
    if (!payUrl || payUrl.trim() === '') {
      handlePaymentFailed('支付链接为空');
      return;
    }

    const div = document.createElement('div');
    div.innerHTML = payUrl;
    document.body.appendChild(div);
    (div.querySelector('form') as HTMLFormElement)?.submit();
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
    cleanup,
    redirectToPay
  };
}