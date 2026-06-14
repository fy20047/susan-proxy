package com.fy20047.susan.service;

import com.fy20047.susan.domain.ItemStatus;
import com.fy20047.susan.domain.ShippingStatus;

public final class StatusResolver {

    private StatusResolver() {
    }

    public static ItemStatus determineLegacy(
            boolean isReconciled,
            boolean isPurchased,
            boolean isArrived,
            boolean isShipped) {
        if (isShipped) {
            return ItemStatus.SHIPPED;
        }
        if (isArrived && isPurchased && isReconciled) {
            return ItemStatus.ARRIVED;
        }
        if (isPurchased && isReconciled) {
            return ItemStatus.IN_TRANSIT;
        }
        if (isPurchased) {
            return ItemStatus.PENDING_DEPOSIT;
        }
        if (isReconciled) {
            return ItemStatus.PENDING_PURCHASE;
        }
        return ItemStatus.REGISTERED;
    }

    public static ItemStatus determineStandard(
            String itemName,
            boolean isPurchased,
            String depositPaidDate,
            boolean isCheckedIn) {
        if (isBlank(itemName)) {
            return ItemStatus.REGISTERED;
        }
        if (!isPurchased) {
            return ItemStatus.PENDING_PURCHASE;
        }
        if (isBlank(depositPaidDate) || !isCheckedIn) {
            return ItemStatus.PENDING_DEPOSIT;
        }
        return ItemStatus.IN_TRANSIT;
    }

    public static ShippingStatus determineShipping(boolean isArrived, boolean isShipped) {
        if (isShipped) {
            return ShippingStatus.SHIPPED;
        }
        if (isArrived) {
            return ShippingStatus.READY_TO_SHIP;
        }
        return ShippingStatus.NOT_ARRIVED;
    }

    public static ShippingStatus determinePreorderShipping(String rawStatus, boolean isShipped) {
        if (isShipped) {
            return ShippingStatus.SHIPPED;
        }
        if (rawStatus == null) {
            return ShippingStatus.NOT_ARRIVED;
        }

        return switch (rawStatus.trim()) {
            case "已出貨" -> ShippingStatus.SHIPPED;
            case "已抵台" -> ShippingStatus.READY_TO_SHIP;
            default -> ShippingStatus.NOT_ARRIVED;
        };
    }

    public static ItemStatus determinePreorder(String rawStatus) {
        return determinePreorder(rawStatus, false);
    }

    public static ItemStatus determinePreorder(String rawStatus, boolean isShipped) {
        if (isShipped) {
            return ItemStatus.PREORDER_SHIPPED;
        }

        if (rawStatus == null) {
            return ItemStatus.PREORDER_REGISTERED;
        }

        String normalized = rawStatus.trim();
        if (normalized.isEmpty()) {
            return ItemStatus.PREORDER_REGISTERED;
        }

        return switch (normalized) {
            case "待購入" -> ItemStatus.PREORDER_PENDING_PURCHASE;
            case "待匯定" -> ItemStatus.PREORDER_PENDING_DEPOSIT;
            case "已購入" -> ItemStatus.PREORDER_PURCHASED;
            case "轉送中" -> ItemStatus.PREORDER_FORWARDING;
            case "已抵台" -> ItemStatus.PREORDER_ARRIVED;
            case "已出貨" -> ItemStatus.PREORDER_SHIPPED;
            case "已登記" -> ItemStatus.PREORDER_REGISTERED;
            default -> ItemStatus.PREORDER_REGISTERED;
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
