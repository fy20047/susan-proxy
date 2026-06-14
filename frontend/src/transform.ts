import { ApiOrderGroup, OrderItemView, OrderView, SourceType } from "./types";
import {
  derivePreorderShippingStatusCode,
  deriveStandardOrderStatusCode,
  toItemStatusLabel,
  toShippingStatusCode,
  toShippingStatusLabel,
  toStandardOrderStatusLabel
} from "./status";

export function buildOrderView(group: ApiOrderGroup): OrderView {
  const sourceType: SourceType = group.sourceType ?? "STANDARD";
  const items: OrderItemView[] = group.items.map((item) => {
    const statusCode =
      item.itemStatus ?? (sourceType === "PREORDER" ? "PREORDER_REGISTERED" : "REGISTERED");
    const rawCheckMark = item.checkMark ?? "";
    const normalizedCheckMark = rawCheckMark.trim();
    const rawDepositPaidDate = item.depositPaidDate ?? "";
    const normalizedDepositPaidDate = rawDepositPaidDate.trim();
    const rawBalanceDueDate = item.balanceDueDate ?? "";
    const normalizedBalanceDueDate = rawBalanceDueDate.trim();
    const isCheckedIn = item.checkedIn ?? false;
    const isDepositPaid = normalizedDepositPaidDate.length > 0;

    return {
      id: item.id,
      name: item.itemName,
      orderSn: item.orderSn,
      orderRank: item.orderRank,
      queued: item.queued,
      checkedIn: isCheckedIn,
      quantity: item.quantity ?? 1,
      totalAmount: item.totalAmount ?? 0,
      depositAmount: item.depositAmount ?? 0,
      balanceAmount: item.balanceAmount ?? 0,
      balanceDueDate: normalizedBalanceDueDate || undefined,
      checkMark: normalizedCheckMark || undefined,
      depositReconciled: item.depositReconciled,
      isPurchased: item.purchased ?? false,
      isDepositPaid,
      isBalancePaid: normalizedBalanceDueDate.length > 0,
      jpyPrice: item.jpyPrice,
      statusCode,
      status: toItemStatusLabel(statusCode),
      shippingStatusCode: item.shippingStatus ?? toShippingStatusCode(statusCode),
      shippingStatus: toShippingStatusLabel(item.shippingStatus ?? toShippingStatusCode(statusCode))
    };
  });

  return rebuildOrderView({
    id: group.id,
    sourceType,
    sourceKey: group.sourceKey,
    groupName: group.groupName ?? "未命名團",
    buyerNickname: group.buyerNickname,
    summaryStatusCode: sourceType === "PREORDER" ? "NOT_ARRIVED" : "REGISTERED",
    summaryStatus: sourceType === "PREORDER" ? "未抵台" : "已登記",
    bonusCount: group.bonusCount ?? 0,
    items,
    totalAmount: 0,
    purchasedQuantity: 0,
    depositAmount: 0,
    paidDepositAmount: 0,
    pendingDepositAmount: 0,
    balanceAmount: 0,
    isBalancePaid: false,
    lastUpdated: group.lastUpdated
  });
}

export function rebuildOrderView(order: OrderView, items: OrderItemView[] = order.items): OrderView {
  const totalAmount = items.reduce((sum, item) => sum + item.totalAmount, 0);
  const purchasedQuantity = items.reduce(
    (sum, item) => sum + (item.isPurchased ? item.quantity : 0),
    0
  );
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
  const isBalancePaid =
    order.items.length > 0 && order.items.every((item) => item.isBalancePaid);

  if (order.sourceType === "PREORDER") {
    const summaryStatusCode = derivePreorderShippingStatusCode(items);
    return {
      ...order,
      items,
      totalAmount,
      purchasedQuantity,
      depositAmount,
      paidDepositAmount,
      pendingDepositAmount,
      balanceAmount,
      isBalancePaid,
      summaryStatusCode,
      summaryStatus: toShippingStatusLabel(summaryStatusCode)
    };
  }

  const summaryStatusCode = deriveStandardOrderStatusCode(items);
  return {
    ...order,
    items,
    totalAmount,
    purchasedQuantity,
    depositAmount,
    paidDepositAmount,
    pendingDepositAmount,
    balanceAmount,
    isBalancePaid,
    summaryStatusCode,
    summaryStatus: toStandardOrderStatusLabel(summaryStatusCode)
  };
}
