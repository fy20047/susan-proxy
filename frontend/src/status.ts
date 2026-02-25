import { ApiOrderItem, ItemStatusCode, ItemStatusLabel, OrderStatus } from "./types";

export type StatusFilter = {
  key: ItemStatusCode | "ALL";
  label: string;
};

export const STATUS_FILTERS: StatusFilter[] = [
  { key: "ALL", label: "全部" },
  { key: "REGISTERED", label: "已喊單" },
  { key: "PENDING_DEPOSIT", label: "待匯定" },
  { key: "PENDING_PURCHASE", label: "待購入" },
  { key: "IN_TRANSIT", label: "運送中" },
  { key: "ARRIVED", label: "抵台待出貨" },
  { key: "SHIPPED", label: "已出貨" }
];

const STATUS_PRIORITY: Record<ItemStatusCode, number> = {
  REGISTERED: 1,
  PENDING_DEPOSIT: 2,
  PENDING_PURCHASE: 3,
  IN_TRANSIT: 4,
  ARRIVED: 5,
  SHIPPED: 6
};

export function toItemStatusLabel(code?: ItemStatusCode): ItemStatusLabel {
  switch (code) {
    case "PENDING_DEPOSIT":
      return "待匯定";
    case "PENDING_PURCHASE":
      return "待購入";
    case "IN_TRANSIT":
      return "運送中";
    case "ARRIVED":
      return "抵台待出貨";
    case "SHIPPED":
      return "已出貨";
    case "REGISTERED":
    default:
      return "已喊單";
  }
}

export function deriveOrderStatusCode(items: ApiOrderItem[]): ItemStatusCode {
  if (!items.length) {
    return "REGISTERED";
  }
  let highest: ItemStatusCode = "REGISTERED";
  for (const item of items) {
    if (!item.itemStatus) continue;
    if (STATUS_PRIORITY[item.itemStatus] > STATUS_PRIORITY[highest]) {
      highest = item.itemStatus;
    }
  }
  return highest;
}

export function toOrderStatusLabel(code: ItemStatusCode): OrderStatus {
  return toItemStatusLabel(code);
}

export function getOrderStatusClass(code: ItemStatusCode): string {
  switch (code) {
    case "REGISTERED":
      return "bg-[#EBE3CC] text-[#2C1E16]";
    case "PENDING_DEPOSIT":
      return "bg-[#D9A036] text-[#2C1E16]";
    case "PENDING_PURCHASE":
      return "bg-[#2A5C5B] text-[#EBE3CC]";
    case "IN_TRANSIT":
      return "bg-[#5B8266] text-[#EBE3CC]";
    case "ARRIVED":
      return "bg-[#BC4A3C] text-[#EBE3CC]";
    case "SHIPPED":
      return "bg-[#2C1E16] text-[#EBE3CC]";
    default:
      return "bg-gray-200 text-black";
  }
}

export function getItemStatusClass(status: ItemStatusLabel): string {
  switch (status) {
    case "已出貨":
    case "抵台待出貨":
      return "text-[10px] md:text-xs px-1.5 py-0.5 border border-[#2C1E16] font-bold bg-[#BC4A3C] text-[#EBE3CC] shadow-[1px_1px_0px_#2C1E16] flex-shrink-0";
    case "運送中":
      return "text-[10px] md:text-xs px-1.5 py-0.5 border border-[#2C1E16] font-bold bg-[#5B8266] text-[#EBE3CC] shadow-[1px_1px_0px_#2C1E16] flex-shrink-0";
    case "待購入":
      return "text-[10px] md:text-xs px-1.5 py-0.5 border border-[#2C1E16] font-bold bg-[#2A5C5B] text-[#EBE3CC] shadow-[1px_1px_0px_#2C1E16] flex-shrink-0";
    case "待匯定":
      return "text-[10px] md:text-xs px-1.5 py-0.5 border border-[#2C1E16] font-bold bg-[#D9A036] text-[#2C1E16] shadow-[1px_1px_0px_#2C1E16] flex-shrink-0";
    case "已喊單":
    default:
      return "text-[10px] md:text-xs px-1.5 py-0.5 border border-[#2C1E16] font-bold bg-[#EBE3CC] text-[#2C1E16] shadow-[1px_1px_0px_#2C1E16] flex-shrink-0";
  }
}
