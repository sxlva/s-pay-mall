export interface CartItem {
  id: number;
  productId: number;
  productName: string;
  productPrice: number;
  quantity: number;
  selected: boolean;
  itemAmount: number;
}

export interface CartState {
  items: CartItem[];
  loading: boolean;
  totalAmount: number;
  totalCount: number;
  allSelected: boolean;
}
