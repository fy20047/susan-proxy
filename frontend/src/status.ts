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

type StatusCarrier =
  | { itemStatus?: ItemStatusCode; shippingStatusCode?: ShippingStatusCode }
  | { statusCode?: ItemStatusCode; shippingStatusCode?: ShippingStatusCode };

export type StandardStatusFilterKey =
  | "ALL"
  | "UNPURCHASED"
  | "PENDING_PAYMENT"
  | "FORWARDING"
  | "READY_TO_SHIP"
  | "SHIPPED";

export type PreorderStatusFilterKey =
  | "ALL"
  | "UNPURCHASED"
  | "PENDING_PAYMENT"
  | "WAITING_OFFICIAL"
  | "FORWARDING"
  | "READY_TO_SHIP"
  | "SHIPPED";

export type StandardFilter = {
  key: StandardStatusFilterKey;
  label: StandardOrderStatus | "全部";
};

export type PreorderItemFilter = {
  key: PreorderStatusFilterKey;
  label: ItemStatusLabel | "全部";
};

export const STANDARD_STATUS_FILTERS: StandardFilter[] = [
  { key: "ALL", label: "全部" },
  { key: "PENDING_PAYMENT", label: "待付款" },
  { key: "UNPURCHASED", label: "尚未購入" },
  { key: "FORWARDING", label: "轉送中" },
  { key: "READY_TO_SHIP", label: "可出貨" },
  { key: "SHIPPED", label: "已出貨" }
];

export const PREORDER_ITEM_FILTERS: PreorderItemFilter[] = [
  { key: "ALL", label: "全部" },
  { key: "PENDING_PAYMENT", label: "待付款" },
  { key: "UNPURCHASED", label: "尚未購入" },
  { key: "WAITING_OFFICIAL", label: "等待官方出貨" },
  { key: "FORWARDING", label: "轉送中" },
  { key: "READY_TO_SHIP", label: "可出貨" },
  { key: "SHIPPED", label: "已出貨" }
];

const STANDARD_STATUS_PRIORITY: Record<StandardItemStatusCode, number> = {
  REGISTERED: 1,
  PENDING_DEPOSIT: 1,
  PENDING_PURCHASE: 2,
  IN_TRANSIT: 3,
  ARRIVED: 4,
  SHIPPED: 5
};

const PREORDER_STATUS_PRIORITY: Record<PreorderItemStatusCode, number> = {
  PREORDER_REGISTERED: 1,
  PREORDER_PENDING_DEPOSIT: 1,
  PREORDER_PENDING_PURCHASE: 2,
  PREORDER_PURCHASED: 3,
  PREORDER_FORWARDING: 4,
  PREORDER_ARRIVED: 5,
  PREORDER_SHIPPED: 6
};

export function isPreorderItemStatus(code?: ItemStatusCode): code is PreorderItemStatusCode {
  return Boolean(code && code.startsWith("PREORDER_"));
}

export function toItemStatusLabel(code?: ItemStatusCode): ItemStatusLabel {
  switch (code) {
    case "PENDING_DEPOSIT":
    case "REGISTERED":
    case "PREORDER_PENDING_DEPOSIT":
    case "PREORDER_REGISTERED":
      return "待付款";
    case "PENDING_PURCHASE":
    case "PREORDER_PENDING_PURCHASE":
      return "尚未購入";
    case "IN_TRANSIT":
    case "PREORDER_FORWARDING":
      return "轉送中";
    case "ARRIVED":
    case "PREORDER_ARRIVED":
      return "可出貨";
    case "SHIPPED":
    case "PREORDER_SHIPPED":
      return "已出貨";
    case "PREORDER_PURCHASED":
      return "等待官方出貨";
    default:
      return "尚未購入";
  }
}

export function toStandardOrderStatusLabel(code: StandardItemStatusCode): StandardOrderStatus {
  return toItemStatusLabel(code) as StandardOrderStatus;
}

export function toShippingStatusCode(code?: ItemStatusCode): ShippingStatusCode {
  switch (code) {
    case "ARRIVED":
    case "PREORDER_ARRIVED":
      return "READY_TO_SHIP";
    case "SHIPPED":
    case "PREORDER_SHIPPED":
      return "SHIPPED";
    default:
      return "NOT_ARRIVED";
  }
}

export function toShippingStatusLabel(code: ShippingStatusCode): ShippingStatusLabel {
  switch (code) {
    case "READY_TO_SHIP":
      return "已抵台待出貨";
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

export function derivePreorderOrderStatusCode(items: StatusCarrier[]): PreorderItemStatusCode {
  let highest: PreorderItemStatusCode = "PREORDER_REGISTERED";
  for (const item of items) {
    const statusCode = getStatusCode(item);
    if (!isPreorderItemStatus(statusCode)) {
      continue;
    }
    if (PREORDER_STATUS_PRIORITY[statusCode] > PREORDER_STATUS_PRIORITY[highest]) {
      highest = statusCode;
    }
  }
  return highest;
}

export function getSummaryStatusClass(code: SummaryStatusCode): string {
  switch (code) {
    case "SHIPPED":
    case "PREORDER_SHIPPED":
      return "bg-[#2C1E16] text-[#EBE3CC]";
    case "ARRIVED":
    case "PREORDER_ARRIVED":
    case "READY_TO_SHIP":
      return "bg-[#BC4A3C] text-[#EBE3CC]";
    case "IN_TRANSIT":
    case "PREORDER_FORWARDING":
      return "bg-[#5B8266] text-[#EBE3CC]";
    case "PREORDER_PURCHASED":
      return "bg-[#D9A036] text-[#2C1E16]";
    case "PENDING_DEPOSIT":
    case "PREORDER_PENDING_DEPOSIT":
      return "bg-[#D9A036] text-[#2C1E16]";
    case "PENDING_PURCHASE":
    case "REGISTERED":
    case "PREORDER_PENDING_PURCHASE":
    case "PREORDER_REGISTERED":
    case "NOT_ARRIVED":
      return "bg-[#EBE3CC] text-[#2C1E16]";
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
    case "可出貨":
      return `${base} bg-[#BC4A3C] text-[#EBE3CC]`;
    case "轉送中":
      return `${base} bg-[#5B8266] text-[#EBE3CC]`;
    case "等待官方出貨":
    case "待付款":
      return `${base} bg-[#D9A036] text-[#2C1E16]`;
    case "尚未購入":
    default:
      return `${base} bg-[#EBE3CC] text-[#2C1E16]`;
  }
}

export function matchesStandardFilter(
  item: OrderItemView,
  itemFilter: StandardStatusFilterKey
): boolean {
  return itemFilter === "ALL" || toStandardFilterKey(item.statusCode) === itemFilter;
}

export function matchesPreorderFilters(
  item: OrderItemView,
  itemFilter: PreorderStatusFilterKey
): boolean {
  return itemFilter === "ALL" || toPreorderFilterKey(item.statusCode) === itemFilter;
}

function toStandardFilterKey(code: ItemStatusCode): StandardStatusFilterKey {
  switch (code) {
    case "PENDING_DEPOSIT":
    case "REGISTERED":
      return "PENDING_PAYMENT";
    case "IN_TRANSIT":
      return "FORWARDING";
    case "ARRIVED":
      return "READY_TO_SHIP";
    case "SHIPPED":
      return "SHIPPED";
    case "PENDING_PURCHASE":
    default:
      return "UNPURCHASED";
  }
}

function toPreorderFilterKey(code: ItemStatusCode): PreorderStatusFilterKey {
  switch (code) {
    case "PREORDER_PENDING_DEPOSIT":
    case "PREORDER_REGISTERED":
      return "PENDING_PAYMENT";
    case "PREORDER_PURCHASED":
      return "WAITING_OFFICIAL";
    case "PREORDER_FORWARDING":
      return "FORWARDING";
    case "PREORDER_ARRIVED":
      return "READY_TO_SHIP";
    case "PREORDER_SHIPPED":
      return "SHIPPED";
    case "PREORDER_PENDING_PURCHASE":
    default:
      return "UNPURCHASED";
  }
}

function getStatusCode(item: StatusCarrier): ItemStatusCode | undefined {
  if ("itemStatus" in item) {
    return item.itemStatus;
  }
  return (item as { statusCode?: ItemStatusCode }).statusCode;
}
