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
        return determineStandard(itemName, isPurchased, !isBlank(depositPaidDate) && isCheckedIn, ShippingStatus.NOT_ARRIVED);
    }

    public static ItemStatus determineStandard(
            String itemName,
            boolean isPurchased,
            boolean isDepositPaid,
            ShippingStatus shippingStatus) {
        if (isBlank(itemName)) {
            return ItemStatus.REGISTERED;
        }
        if (!isPurchased) {
            return ItemStatus.PENDING_PURCHASE;
        }
        if (!isDepositPaid) {
            return ItemStatus.PENDING_DEPOSIT;
        }
        if (shippingStatus == ShippingStatus.SHIPPED) {
            return ItemStatus.SHIPPED;
        }
        if (shippingStatus == ShippingStatus.READY_TO_SHIP) {
            return ItemStatus.ARRIVED;
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

    public static ShippingStatus determineStandardShipping(
            String rawShippingProgress,
            boolean isArrived,
            boolean isShipped) {
        if (isShipped) {
            return ShippingStatus.SHIPPED;
        }
        if (!isBlank(rawShippingProgress)) {
            return switch (normalize(rawShippingProgress)) {
                case "已出貨" -> ShippingStatus.SHIPPED;
                case "已抵台待出貨", "已抵台可出貨", "已抵台" -> ShippingStatus.READY_TO_SHIP;
                case "尚未抵台", "未抵台" -> ShippingStatus.NOT_ARRIVED;
                default -> ShippingStatus.NOT_ARRIVED;
            };
        }
        return determineShipping(isArrived, false);
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

    public static ShippingStatus determinePreorderShipping(
            String rawShippingProgress,
            String rawStatus,
            boolean isShipped) {
        if (isShipped) {
            return ShippingStatus.SHIPPED;
        }
        if (!isBlank(rawShippingProgress)) {
            return switch (normalize(rawShippingProgress)) {
                case "已出貨" -> ShippingStatus.SHIPPED;
                case "已抵台待出貨", "已抵台可出貨", "已抵台" -> ShippingStatus.READY_TO_SHIP;
                default -> ShippingStatus.NOT_ARRIVED;
            };
        }
        return determinePreorderShipping(rawStatus, false);
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

    public static ItemStatus determinePreorder(
            String itemName,
            boolean isPurchased,
            boolean isDepositPaid,
            String rawShippingProgress,
            String rawStatus,
            boolean isShipped) {
        if (isBlank(itemName)) {
            return ItemStatus.PREORDER_REGISTERED;
        }
        if (!isPurchased) {
            return ItemStatus.PREORDER_PENDING_PURCHASE;
        }
        if (!isDepositPaid) {
            return ItemStatus.PREORDER_PENDING_DEPOSIT;
        }
        if (isShipped) {
            return ItemStatus.PREORDER_SHIPPED;
        }

        if (!isBlank(rawShippingProgress)) {
            return switch (normalize(rawShippingProgress)) {
                case "已出貨" -> ItemStatus.PREORDER_SHIPPED;
                case "已抵台待出貨", "已抵台可出貨", "已抵台" -> ItemStatus.PREORDER_ARRIVED;
                case "官方已發貨" -> ItemStatus.PREORDER_FORWARDING;
                case "已下單待發貨", "尚未抵台", "未抵台" -> ItemStatus.PREORDER_PURCHASED;
                default -> ItemStatus.PREORDER_PURCHASED;
            };
        }

        ItemStatus legacyStatus = determinePreorder(rawStatus, false);
        return switch (legacyStatus) {
            case PREORDER_SHIPPED -> ItemStatus.PREORDER_SHIPPED;
            case PREORDER_ARRIVED -> ItemStatus.PREORDER_ARRIVED;
            case PREORDER_FORWARDING -> ItemStatus.PREORDER_FORWARDING;
            case PREORDER_PURCHASED -> ItemStatus.PREORDER_PURCHASED;
            default -> ItemStatus.PREORDER_PURCHASED;
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
