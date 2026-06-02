export type OrderStatus = 'INIT' | 'PAID' | 'SHIPPED' | 'DONE' | 'CANCELED';

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  price: number;
  quantity: number;
  itemAmount: number;
}

export interface Order {
  id: number;
  orderNo: string;
  userId: number;
  totalAmount: number;
  address: string;
  status: OrderStatus;
  statusDesc: string;
  createTime: string;
  updateTime: string;
  totalCount: number;
  items: OrderItem[];
}

export interface OrderCreateResult {
  orderNo: string;
  totalAmount: number;
  status: string;
  payUrl: string | null;
}

export interface OrderState {
  orders: Order[];
  currentOrder: Order | null;
  loading: boolean;
}

export interface OrderListParams {
  status?: string;
  startTime?: string;
  endTime?: string;
}