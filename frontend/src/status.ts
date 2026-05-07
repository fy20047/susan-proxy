import {
  ItemStatusCode,
  ItemStatusLabel,
  OrderItemView,
  PreorderItemStatusCode,
  ShippingStatusCode,
  ShippingStatusLabel,
  StandardItemStatusCode,
  StandardOrderStatus,
  SummaryStatusCode
} from "./types";

type StatusCarrier = { itemStatus?: ItemStatusCode } | { statusCode?: ItemStatusCode };

export type StandardFilter = {
  key: StandardItemStatusCode | "ALL";
  label: string;
};

export type PreorderItemFilter = {
  key: PreorderItemStatusCode | "ALL";
  label: string;
};

export type ShippingFilter = {
  key: ShippingStatusCode | "ALL";
  label: string;
};

export const STANDARD_STATUS_FILTERS: StandardFilter[] = [
  { key: "ALL", label: "全部" },
  { key: "REGISTERED", label: "已登記" },
  { key: "PENDING_DEPOSIT", label: "待匯定" },
  { key: "PENDING_PURCHASE", label: "待購入" },
  { key: "IN_TRANSIT", label: "運送中" },
  { key: "ARRIVED", label: "已抵台待出貨" },
  { key: "SHIPPED", label: "已出貨" }
];

export const PREORDER_ITEM_FILTERS: PreorderItemFilter[] = [
  { key: "ALL", label: "全部" },
  { key: "PREORDER_REGISTERED", label: "已登記" },
  { key: "PREORDER_PENDING_PURCHASE", label: "待購入" },
  { key: "PREORDER_PENDING_DEPOSIT", label: "待匯定" },
  { key: "PREORDER_PURCHASED", label: "已購入" },
  { key: "PREORDER_FORWARDING", label: "轉送中" },
  { key: "PREORDER_ARRIVED", label: "已抵台" },
  { key: "PREORDER_SHIPPED", label: "已出貨" }
];

export const PREORDER_SHIPPING_FILTERS: ShippingFilter[] = [
  { key: "ALL", label: "全部" },
  { key: "NOT_ARRIVED", label: "尚未抵台" },
  { key: "READY_TO_SHIP", label: "可下單等待出貨" },
  { key: "SHIPPED", label: "已出貨" }
];

const STANDARD_STATUS_PRIORITY: Record<StandardItemStatusCode, number> = {
  REGISTERED: 1,
  PENDING_DEPOSIT: 2,
  PENDING_PURCHASE: 3,
  IN_TRANSIT: 4,
  ARRIVED: 5,
  SHIPPED: 6
};

const SHIPPING_PRIORITY: Record<ShippingStatusCode, number> = {
  NOT_ARRIVED: 1,
  READY_TO_SHIP: 2,
  SHIPPED: 3
};

export function isPreorderItemStatus(code?: ItemStatusCode): code is PreorderItemStatusCode {
  return Boolean(code && code.startsWith("PREORDER_"));
}

export function toItemStatusLabel(code?: ItemStatusCode): ItemStatusLabel {
  switch (code) {
    case "PENDING_DEPOSIT":
      return "待匯定";
    case "PENDING_PURCHASE":
      return "待購入";
    case "IN_TRANSIT":
      return "運送中";
    case "ARRIVED":
      return "已抵台待出貨";
    case "SHIPPED":
      return "已出貨";
    case "PREORDER_PENDING_PURCHASE":
      return "待購入";
    case "PREORDER_PENDING_DEPOSIT":
      return "待匯定";
    case "PREORDER_PURCHASED":
      return "已購入";
    case "PREORDER_FORWARDING":
      return "轉送中";
    case "PREORDER_ARRIVED":
      return "已抵台";
    case "PREORDER_SHIPPED":
      return "已出貨";
    case "PREORDER_REGISTERED":
      return "已登記";
    case "REGISTERED":
    default:
      return "已登記";
  }
}

export function toStandardOrderStatusLabel(code: StandardItemStatusCode): StandardOrderStatus {
  return toItemStatusLabel(code) as StandardOrderStatus;
}

export function toShippingStatusCode(code?: ItemStatusCode): ShippingStatusCode {
  switch (code) {
    case "PREORDER_ARRIVED":
      return "READY_TO_SHIP";
    case "PREORDER_SHIPPED":
      return "SHIPPED";
    default:
      return "NOT_ARRIVED";
  }
}

export function toShippingStatusLabel(code: ShippingStatusCode): ShippingStatusLabel {
  switch (code) {
    case "READY_TO_SHIP":
      return "可下單等待出貨";
    case "SHIPPED":
      return "已出貨";
    case "NOT_ARRIVED":
    default:
      return "尚未抵台";
  }
}

export function deriveStandardOrderStatusCode(items: StatusCarrier[]): StandardItemStatusCode {
  let highest: StandardItemStatusCode = "REGISTERED";
  for (const item of items) {
    const statusCode = getStatusCode(item);
    if (!statusCode || isPreorderItemStatus(statusCode)) {
      continue;
    }
    if (STANDARD_STATUS_PRIORITY[statusCode] > STANDARD_STATUS_PRIORITY[highest]) {
      highest = statusCode;
    }
  }
  return highest;
}

export function derivePreorderShippingStatusCode(items: StatusCarrier[]): ShippingStatusCode {
  let highest: ShippingStatusCode = "NOT_ARRIVED";
  for (const item of items) {
    const shippingStatusCode = toShippingStatusCode(getStatusCode(item));
    if (SHIPPING_PRIORITY[shippingStatusCode] > SHIPPING_PRIORITY[highest]) {
      highest = shippingStatusCode;
    }
  }
  return highest;
}

export function getSummaryStatusClass(code: SummaryStatusCode): string {
  switch (code) {
    case "REGISTERED":
    case "NOT_ARRIVED":
      return "bg-[#EBE3CC] text-[#2C1E16]";
    case "PENDING_DEPOSIT":
      return "bg-[#D9A036] text-[#2C1E16]";
    case "PENDING_PURCHASE":
      return "bg-[#2A5C5B] text-[#EBE3CC]";
    case "IN_TRANSIT":
      return "bg-[#5B8266] text-[#EBE3CC]";
    case "ARRIVED":
    case "READY_TO_SHIP":
      return "bg-[#BC4A3C] text-[#EBE3CC]";
    case "SHIPPED":
      return "bg-[#2C1E16] text-[#EBE3CC]";
    default:
      return "bg-gray-200 text-black";
  }
}

export function getItemStatusClass(status: ItemStatusLabel): string {
  const base =
    "text-[10px] md:text-xs px-1.5 py-0.5 border border-[#2C1E16] font-bold shadow-[1px_1px_0px_#2C1E16] flex-shrink-0";

  switch (status) {
    case "已出貨":
      return `${base} bg-[#2C1E16] text-[#EBE3CC]`;
    case "已抵台待出貨":
    case "已抵台":
      return `${base} bg-[#BC4A3C] text-[#EBE3CC]`;
    case "運送中":
    case "轉送中":
      return `${base} bg-[#5B8266] text-[#EBE3CC]`;
    case "待購入":
      return `${base} bg-[#2A5C5B] text-[#EBE3CC]`;
    case "待匯定":
    case "已購入":
      return `${base} bg-[#D9A036] text-[#2C1E16]`;
    case "已登記":
    default:
      return `${base} bg-[#EBE3CC] text-[#2C1E16]`;
  }
}

export function matchesPreorderFilters(
  item: OrderItemView,
  itemFilter: PreorderItemStatusCode | "ALL",
  shippingFilter: ShippingStatusCode | "ALL"
): boolean {
  if (itemFilter !== "ALL" && item.statusCode !== itemFilter) {
    return false;
  }
  if (shippingFilter !== "ALL" && item.shippingStatusCode !== shippingFilter) {
    return false;
  }
  return true;
}

function getStatusCode(item: StatusCarrier): ItemStatusCode | undefined {
  if ("itemStatus" in item) {
    return item.itemStatus;
  }
  return (item as { statusCode?: ItemStatusCode }).statusCode;
}
