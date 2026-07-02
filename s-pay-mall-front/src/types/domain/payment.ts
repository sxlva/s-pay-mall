/**
 * 支付类型定义
 *
 * @author 傅崇睿
 */

/** 支付状态：WAIT_PAY-待支付 PAYING-支付中 PAID-已支付 CLOSED-已关闭 FAILED-支付失败 */
export type PayStatus = 'WAIT_PAY' | 'PAYING' | 'PAID' | 'CLOSED' | 'FAILED';

/** 支付订单 */
export interface PayOrder {
  /** 订单号 */
  orderNo: string;
  /** 用户 ID */
  userId: string;
  /** 商品 ID */
  productId: string;
  /** 商品名称 */
  productName: string;
  /** 支付金额 */
  totalAmount: number;
  /** 支付状态 */
  status: PayStatus;
  /** 支付状态描述 */
  statusDesc: string;
  /** 支付链接 */
  payUrl: string | null;
  /** 交易流水号 */
  tradeNo: string | null;
  /** 支付时间 */
  payTime: string | null;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
}

/** 支付结果 */
export interface PayResult {
  /** 订单号 */
  orderNo: string;
  /** 支付金额 */
  totalAmount: number;
  /** 支付状态 */
  status: string;
  /** 支付链接 */
  payUrl: string;
}

/** 支付创建参数 */
export interface PayCreateParams {
  /** 用户 ID */
  userId: string;
  /** 商品 ID */
  productId: string;
}

/** 支付轮询状态：idle-空闲 polling-轮询中 success-成功 failed-失败 timeout-超时 */
export type PollingState = 'idle' | 'polling' | 'success' | 'failed' | 'timeout';

/** 支付状态（Store 层） */
export interface PaymentState {
  /** 当前支付订单 */
  currentPayOrder: PayOrder | null;
  /** 支付链接 */
  payUrl: string | null;
  /** 轮询状态 */
  pollingState: PollingState;
  /** 已轮询次数 */
  pollingCount: number;
  /** 倒计时秒数 */
  countdown: number;
  /** 错误信息 */
  error: string | null;
}