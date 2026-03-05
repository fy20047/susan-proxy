import { ApiOrderGroup, OrderItemView, OrderView } from "./types";
import { deriveOrderStatusCode, toItemStatusLabel, toOrderStatusLabel } from "./status";

export function buildOrderView(group: ApiOrderGroup): OrderView {
  const items: OrderItemView[] = group.items.map((item) => {
    const statusCode = item.itemStatus ?? "REGISTERED";
    const rawCheckMark = item.checkMark ?? "";
    const normalizedCheckMark = rawCheckMark.trim();
    const isDepositPaid = normalizedCheckMark.length > 0;
    return {
      id: item.id,
      name: item.itemName,
      orderSn: item.orderSn,
      queued: item.queued ?? false,
      checkedIn: item.checkedIn ?? false,
      quantity: item.quantity ?? 1,
      totalAmount: item.totalAmount ?? 0,
      depositAmount: item.depositAmount ?? 0,
      balanceAmount: item.balanceAmount ?? 0,
      checkMark: normalizedCheckMark || undefined,
      isDepositPaid,
      jpyPrice: item.jpyPrice,
      statusCode,
      status: toItemStatusLabel(statusCode)
    };
  });

  const totalAmount = items.reduce((sum, item) => sum + item.totalAmount, 0);
  const depositAmount = items.reduce((sum, item) => sum + item.depositAmount, 0);
  const paidDepositAmount = items.reduce(
    (sum, item) => sum + (item.isDepositPaid ? item.depositAmount : 0),
    0
  );
  const pendingDepositAmount = items.reduce(
    (sum, item) => sum + (item.isDepositPaid ? 0 : item.depositAmount),
    0
  );
  const balanceAmount = items.reduce((sum, item) => sum + item.balanceAmount, 0);

  const statusCode = deriveOrderStatusCode(group.items);
  return {
    id: group.id,
    groupName: group.groupName ?? "未命名團",
    buyerNickname: group.buyerNickname,
    statusCode,
    status: toOrderStatusLabel(statusCode),
    bonusCount: group.bonusCount ?? 0,
    items,
    totalAmount,
    depositAmount,
    paidDepositAmount,
    pendingDepositAmount,
    balanceAmount,
    lastUpdated: group.lastUpdated
  };
}
