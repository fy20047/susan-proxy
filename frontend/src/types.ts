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

export type PageViewStats = {
  daily: number;
  monthly: number;
  total: number;
};

export type ItemStatusCode =
  | "REGISTERED"
  | "PENDING_DEPOSIT"
  | "PENDING_PURCHASE"
  | "IN_TRANSIT"
  | "ARRIVED"
  | "SHIPPED";

export type OrderStatus =
  | "已登記"
  | "待匯定"
  | "待購入"
  | "運送中"
  | "已抵台待出貨"
  | "已出貨";

export type ItemStatusLabel = OrderStatus;

export type ApiOrderItem = {
  id: number;
  orderSn?: string;
  queued?: boolean;
  checkedIn?: boolean;
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
  bonusCount?: number;
  items: ApiOrderItem[];
};

export type OrderItemView = {
  id: number;
  name: string;
  orderSn?: string;
  queued?: boolean;
  checkedIn?: boolean;
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
