package com.fy20047.susan.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fy20047.susan.domain.ItemStatus;
import org.junit.jupiter.api.Test;

class StatusResolverTest {

    @Test
    void shouldRemainPendingDepositWhenArrivedCheckedButNotReconciled() {
        ItemStatus status = StatusResolver.determine(false, true, true, false);

        assertEquals(ItemStatus.PENDING_DEPOSIT, status);
    }

    @Test
    void shouldBeArrivedOnlyWhenReconciledAndPurchased() {
        ItemStatus status = StatusResolver.determine(true, true, true, false);

        assertEquals(ItemStatus.ARRIVED, status);
    }
}
