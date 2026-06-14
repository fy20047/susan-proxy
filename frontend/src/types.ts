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
  weekly: number;
  monthly: number;
  total: number;
};

export type SourceType = "STANDARD" | "PREORDER";

export type StandardItemStatusCode =
  | "REGISTERED"
  | "PENDING_DEPOSIT"
  | "PENDING_PURCHASE"
  | "IN_TRANSIT"
  | "ARRIVED"
  | "SHIPPED";

export type PreorderItemStatusCode =
  | "PREORDER_REGISTERED"
  | "PREORDER_PENDING_PURCHASE"
  | "PREORDER_PENDING_DEPOSIT"
  | "PREORDER_PURCHASED"
  | "PREORDER_FORWARDING"
  | "PREORDER_ARRIVED"
  | "PREORDER_SHIPPED";

export type ItemStatusCode = StandardItemStatusCode | PreorderItemStatusCode;

export type ShippingStatusCode = "NOT_ARRIVED" | "READY_TO_SHIP" | "SHIPPED";

export type StandardOrderStatus =
  | "尚未購入"
  | "待付款"
  | "轉送中"
  | "可出貨"
  | "已出貨";

export type PreorderItemStatusLabel =
  | "尚未購入"
  | "待付款"
  | "等待官方出貨"
  | "轉送中"
  | "可出貨"
  | "已出貨";

export type ShippingStatusLabel = "尚未抵台" | "已抵台待出貨" | "已出貨";

export type ItemStatusLabel = StandardOrderStatus | PreorderItemStatusLabel;
export type SummaryStatusCode = ItemStatusCode | ShippingStatusCode;
export type SummaryStatusLabel = ItemStatusLabel | ShippingStatusLabel;

export type ApiOrderItem = {
  id: number;
  orderSn?: string;
  orderRank?: string;
  queued?: boolean;
  checkedIn?: boolean;
  balanceDueDate?: string;
  depositPaidDate?: string;
  depositPaid?: boolean;
  depositReconciled?: boolean;
  purchased?: boolean;
  checkMark?: string;
  depositAmount?: number;
  balanceAmount?: number;
  totalAmount?: number;
  itemName: string;
  quantity?: number;
  jpyPrice?: number;
  itemStatus?: ItemStatusCode;
  shippingStatus?: ShippingStatusCode;
};

export type ApiOrderGroup = {
  id: number;
  buyerNickname: string;
  groupName?: string;
  sourceKey?: string;
  sourceType?: SourceType;
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
  orderRank?: string;
  queued?: boolean;
  checkedIn?: boolean;
  quantity: number;
  totalAmount: number;
  depositAmount: number;
  balanceAmount: number;
  balanceDueDate?: string;
  checkMark?: string;
  depositReconciled?: boolean;
  isPurchased: boolean;
  isDepositPaid: boolean;
  isBalancePaid: boolean;
  jpyPrice?: number;
  statusCode: ItemStatusCode;
  status: ItemStatusLabel;
  shippingStatusCode?: ShippingStatusCode;
  shippingStatus?: ShippingStatusLabel;
};

export type OrderView = {
  id: number;
  sourceType: SourceType;
  sourceKey?: string;
  groupName: string;
  buyerNickname: string;
  summaryStatusCode: SummaryStatusCode;
  summaryStatus: SummaryStatusLabel;
  bonusCount: number;
  items: OrderItemView[];
  totalAmount: number;
  purchasedQuantity: number;
  depositAmount: number;
  paidDepositAmount: number;
  pendingDepositAmount: number;
  balanceAmount: number;
  isBalancePaid: boolean;
  lastUpdated?: string;
};
