export type PayStatus = 'WAIT_PAY' | 'PAYING' | 'PAID' | 'CLOSED' | 'FAILED';

export interface PayOrder {
  orderNo: string;
  userId: string;
  productId: string;
  productName: string;
  totalAmount: number;
  status: PayStatus;
  statusDesc: string;
  payUrl: string | null;
  tradeNo: string | null;
  payTime: string | null;
  createTime: string;
  updateTime: string;
}

export interface PayResult {
  orderNo: string;
  totalAmount: number;
  status: string;
  payUrl: string;
}

export interface PayCreateParams {
  userId: string;
  productId: string;
}

export type PollingState = 'idle' | 'polling' | 'success' | 'failed' | 'timeout';

export interface PaymentState {
  currentPayOrder: PayOrder | null;
  payUrl: string | null;
  pollingState: PollingState;
  pollingCount: number;
  countdown: number;
  error: string | null;
}