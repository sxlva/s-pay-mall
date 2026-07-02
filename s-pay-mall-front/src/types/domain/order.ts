/**
 * 订单类型定义
 *
 * @author 傅崇睿
 */

/** 订单状态：CREATED-已创建 INIT-初始化 WAIT_PAY-待支付 PAID-已支付 SHIPPED-已发货 DONE-已完成 CANCELED-已取消 */
export type OrderStatus = 'CREATED' | 'INIT' | 'WAIT_PAY' | 'PAID' | 'SHIPPED' | 'DONE' | 'CANCELED';

/** 订单商品项 */
export interface OrderItem {
  /** 订单项 ID */
  id: number;
  /** 商品 ID */
  productId: number;
  /** 商品名称 */
  productName: string;
  /** 商品单价 */
  price: number;
  /** 购买数量 */
  quantity: number;
  /** 小计金额 */
  itemAmount: number;
}

/** 订单实体 */
export interface Order {
  /** 订单 ID */
  id: number;
  /** 订单编号 */
  orderNo: string;
  /** 用户 ID */
  userId: number;
  /** 订单总金额 */
  totalAmount: number;
  /** 收货地址 */
  address: string;
  /** 订单状态 */
  status: OrderStatus;
  /** 订单状态描述 */
  statusDesc: string;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
  /** 商品总数量 */
  totalCount: number;
  /** 订单商品列表 */
  items: OrderItem[];
}

/** 订单创建结果 */
export interface OrderCreateResult {
  /** 订单编号 */
  orderNo: string;
  /** 订单总金额 */
  totalAmount: number;
  /** 订单状态 */
  status: string;
  /** 支付链接（可能为 HTML 表单） */
  payUrl: string | null;
  /** 支付表单 HTML（后端下划线字段） */
  _html?: string;
  /** 支付表单 HTML（后端驼峰字段） */
  html?: string;
}

/** 订单列表状态 */
export interface OrderState {
  /** 订单列表 */
  orders: Order[];
  /** 当前选中的订单 */
  currentOrder: Order | null;
  /** 是否正在加载 */
  loading: boolean;
}

/** 订单列表查询参数 */
export interface OrderListParams {
  /** 按状态过滤 */
  status?: string;
  /** 开始时间 */
  startTime?: string;
  /** 结束时间 */
  endTime?: string;
}