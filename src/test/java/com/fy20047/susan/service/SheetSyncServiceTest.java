package com.fy20047.susan.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fy20047.susan.domain.OrderGroup;
import com.fy20047.susan.domain.OrderItem;
import com.fy20047.susan.repository.OrderGroupRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class SheetSyncServiceTest {

    private static final String RECONCILED = "\u5c0d\u5e33";
    private static final String DEPOSIT_AMOUNT = "\u5b9a\u91d180%";
    private static final String BALANCE_AMOUNT = "\u5c3e\u6b3e20%";
    private static final String TOTAL_AMOUNT = "\u8cfc\u8cb7\u7e3d\u984d";
    private static final String BUYER = "\u5718\u53cb";
    private static final String ITEM = "\u54c1\u9805";
    private static final String QUEUED = "\u662f\u5426\u6392\u5230";
    private static final String LEGACY_QUEUED = "\u5df2\u6392\u5230";

    @TempDir
    Path tempDir;

    @Test
    void syncFromCsvStoresQueuedTrueFromCurrentQueuedHeader() throws IOException {
        OrderItem item = syncSingleItem(List.of(QUEUED), List.of("TRUE"));

        Assertions.assertEquals(true, item.getQueued());
    }

    @Test
    void syncFromCsvStoresQueuedFalseFromCurrentQueuedHeader() throws IOException {
        OrderItem item = syncSingleItem(List.of(QUEUED), List.of("FALSE"));

        Assertions.assertEquals(false, item.getQueued());
    }

    @Test
    void syncFromCsvFallsBackToLegacyQueuedHeader() throws IOException {
        OrderItem item = syncSingleItem(List.of(LEGACY_QUEUED), List.of("TRUE"));

        Assertions.assertEquals(true, item.getQueued());
    }

    @Test
    void syncFromCsvPrefersCurrentQueuedHeaderOverLegacyHeader() throws IOException {
        OrderItem item = syncSingleItem(List.of(QUEUED, LEGACY_QUEUED), List.of("FALSE", "TRUE"));

        Assertions.assertEquals(false, item.getQueued());
    }

    @Test
    void syncFromCsvStoresNullQueuedWhenQueuedHeaderIsMissing() throws IOException {
        OrderItem item = syncSingleItem(List.of(), List.of());

        Assertions.assertNull(item.getQueued());
    }

    private OrderItem syncSingleItem(List<String> extraHeaders, List<String> extraValues) throws IOException {
        OrderGroupRepository repository = mock(OrderGroupRepository.class);
        when(repository.findByGroupNameAndSourceKeyIncludingLegacy(anyString(), anyString())).thenReturn(List.of());
        SheetSyncService service = new SheetSyncService(repository, mock(SheetSyncWriter.class));

        Path csvPath = tempDir.resolve("orders.csv");
        Files.writeString(csvPath, buildCsv(extraHeaders, extraValues), StandardCharsets.UTF_8);

        service.syncFromCsv(csvPath, "Test Group");

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Collection<OrderGroup>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(repository).saveAll(captor.capture());

        OrderGroup group = captor.getValue().iterator().next();
        Assertions.assertEquals("Buyer", group.getBuyerNickname());
        Assertions.assertEquals("Test Group", group.getGroupName());
        Assertions.assertEquals(1, group.getItems().size());
        return group.getItems().getFirst();
    }

    private String buildCsv(List<String> extraHeaders, List<String> extraValues) {
        List<String> headers = new java.util.ArrayList<>(List.of(
                RECONCILED,
                DEPOSIT_AMOUNT,
                BALANCE_AMOUNT,
                TOTAL_AMOUNT,
                BUYER,
                ITEM
        ));
        headers.addAll(extraHeaders);

        List<String> values = new java.util.ArrayList<>(List.of(
                "TRUE",
                "100",
                "25",
                "125",
                "Buyer",
                "Item"
        ));
        values.addAll(extraValues);

        return String.join(",", headers) + System.lineSeparator()
                + String.join(",", values) + System.lineSeparator();
    }
}
