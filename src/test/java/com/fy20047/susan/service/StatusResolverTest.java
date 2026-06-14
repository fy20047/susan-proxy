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
        ItemStatus status = StatusResolver.determineStandard(
                "測試商品",
                false,
                false,
                ShippingStatus.NOT_ARRIVED);

        assertEquals(ItemStatus.PENDING_PURCHASE, status);
    }

    @Test
    void shouldUsePendingPaymentWhenStandardItemIsPurchasedButDepositIsMissing() {
        ItemStatus status = StatusResolver.determineStandard(
                "測試商品",
                true,
                false,
                ShippingStatus.NOT_ARRIVED);

        assertEquals(ItemStatus.PENDING_DEPOSIT, status);
    }

    @Test
    void shouldUseForwardingWhenStandardItemIsPurchasedAndDepositStarted() {
        ItemStatus status = StatusResolver.determineStandard(
                "測試商品",
                true,
                true,
                ShippingStatus.NOT_ARRIVED);

        assertEquals(ItemStatus.IN_TRANSIT, status);
    }

    @Test
    void shouldUseReadyToShipWhenStandardItemArrivedAfterDepositStarted() {
        ItemStatus status = StatusResolver.determineStandard(
                "測試商品",
                true,
                true,
                ShippingStatus.READY_TO_SHIP);

        assertEquals(ItemStatus.ARRIVED, status);
    }

    @Test
    void shouldResolveStandardShippingProgressColumn() {
        assertEquals(ShippingStatus.NOT_ARRIVED,
                StatusResolver.determineStandardShipping("尚未抵台", true, false));
        assertEquals(ShippingStatus.READY_TO_SHIP,
                StatusResolver.determineStandardShipping("已抵台待出貨", false, false));
        assertEquals(ShippingStatus.READY_TO_SHIP,
                StatusResolver.determineStandardShipping("已抵台可出貨", false, false));
        assertEquals(ShippingStatus.SHIPPED,
                StatusResolver.determineStandardShipping("已出貨", false, false));
    }

    @Test
    void shouldResolvePreorderVisibleStatesFromShippingProgress() {
        assertEquals(ItemStatus.PREORDER_PURCHASED, StatusResolver.determinePreorder(
                "測試商品", true, true, "已下單待發貨", "", false));
        assertEquals(ItemStatus.PREORDER_FORWARDING, StatusResolver.determinePreorder(
                "測試商品", true, true, "官方已發貨", "", false));
        assertEquals(ItemStatus.PREORDER_ARRIVED, StatusResolver.determinePreorder(
                "測試商品", true, true, "已抵台待出貨", "", false));
        assertEquals(ItemStatus.PREORDER_SHIPPED, StatusResolver.determinePreorder(
                "測試商品", true, true, "已出貨", "", false));
    }

    @Test
    void shouldResolveShippingStatusSeparately() {
        assertEquals(ShippingStatus.NOT_ARRIVED, StatusResolver.determineShipping(false, false));
        assertEquals(ShippingStatus.READY_TO_SHIP, StatusResolver.determineShipping(true, false));
        assertEquals(ShippingStatus.SHIPPED, StatusResolver.determineShipping(true, true));
    }
}
