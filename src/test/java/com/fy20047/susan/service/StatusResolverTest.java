package com.fy20047.susan.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fy20047.susan.domain.ItemStatus;
import com.fy20047.susan.domain.ShippingStatus;
import org.junit.jupiter.api.Test;

class StatusResolverTest {

    @Test
    void shouldRemainPendingDepositWhenArrivedCheckedButNotReconciled() {
        ItemStatus status = StatusResolver.determineLegacy(false, true, true, false);

        assertEquals(ItemStatus.PENDING_DEPOSIT, status);
    }

    @Test
    void shouldBeArrivedOnlyWhenReconciledAndPurchased() {
        ItemStatus status = StatusResolver.determineLegacy(true, true, true, false);

        assertEquals(ItemStatus.ARRIVED, status);
    }

    @Test
    void shouldMapPreorderStatus() {
        ItemStatus status = StatusResolver.determinePreorder("轉送中");

        assertEquals(ItemStatus.PREORDER_FORWARDING, status);
    }

    @Test
    void shouldUseShippedCheckboxForPreorderStatus() {
        ItemStatus status = StatusResolver.determinePreorder("已抵台", true);

        assertEquals(ItemStatus.PREORDER_SHIPPED, status);
    }

    @Test
    void shouldUsePendingPurchaseWhenStandardItemHasNameButIsNotPurchased() {
        ItemStatus status = StatusResolver.determineStandard("測試商品", false, "", false);

        assertEquals(ItemStatus.PENDING_PURCHASE, status);
    }

    @Test
    void shouldUseForwardingWhenStandardItemIsPurchasedAndDepositCompleted() {
        ItemStatus status = StatusResolver.determineStandard("測試商品", true, "2026/07/01", true);

        assertEquals(ItemStatus.IN_TRANSIT, status);
    }

    @Test
    void shouldResolveShippingStatusSeparately() {
        assertEquals(ShippingStatus.NOT_ARRIVED, StatusResolver.determineShipping(false, false));
        assertEquals(ShippingStatus.READY_TO_SHIP, StatusResolver.determineShipping(true, false));
        assertEquals(ShippingStatus.SHIPPED, StatusResolver.determineShipping(true, true));
    }
}
