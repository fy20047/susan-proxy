package com.fy20047.susan.service;

import com.fy20047.susan.domain.ItemStatus;

public final class StatusResolver {

    private StatusResolver() {
    }

    public static ItemStatus determine(
            boolean isReconciled,
            boolean isPurchased,
            boolean isArrived,
            boolean isShipped) {
        if (isShipped) {
            return ItemStatus.SHIPPED;
        }
        if (isArrived) {
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
}
