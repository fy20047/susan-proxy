export type ApiError = {
  code: string;
  message: string;
};

export type ApiResponse<T> = {
  success: boolean;
  data?: T;
  error?: ApiError;
  timestamp?: string;
};

export type ItemStatusCode =
  | "REGISTERED"
  | "PENDING_DEPOSIT"
  | "PENDING_PURCHASE"
  | "IN_TRANSIT"
  | "ARRIVED"
  | "SHIPPED";

export type OrderStatus =
  | "已喊單"
  | "待匯定"
  | "待購入"
  | "運送中"
  | "抵台待出貨"
  | "已出貨"
  | "已取消";

export type ItemStatusLabel =
  | "已喊單"
  | "待匯定"
  | "待購入"
  | "運送中"
  | "抵台待出貨"
  | "已出貨";

export type ApiOrderItem = {
  id: number;
  orderSn?: string;
  balanceDueDate?: string;
  depositPaidDate?: string;
  depositAmount?: number;
  balanceAmount?: number;
  totalAmount?: number;
  itemName: string;
  quantity?: number;
  jpyPrice?: number;
  itemStatus?: ItemStatusCode;
};

export type ApiOrderGroup = {
  id: number;
  buyerNickname: string;
  groupName?: string;
  lastUpdated?: string;
  totalAmount?: number;
  totalBalance?: number;
  items: ApiOrderItem[];
};

export type OrderItemView = {
  id: number;
  name: string;
  quantity: number;
  totalAmount: number;
  depositAmount: number;
  balanceAmount: number;
  jpyPrice?: number;
  statusCode: ItemStatusCode;
  status: ItemStatusLabel;
};

export type OrderView = {
  id: number;
  groupName: string;
  buyerNickname: string;
  statusCode: ItemStatusCode;
  status: OrderStatus;
  bonusCount: number;
  items: OrderItemView[];
  totalAmount: number;
  depositAmount: number;
  balanceAmount: number;
  lastUpdated?: string;
};
