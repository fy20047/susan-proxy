package com.fy20047.susan.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fy20047.susan.domain.GroupSourceType;
import com.fy20047.susan.domain.OrderGroup;
import com.fy20047.susan.domain.OrderItem;
import com.fy20047.susan.domain.SheetSyncSettings;
import com.fy20047.susan.domain.SheetSyncSource;
import com.fy20047.susan.repository.OrderGroupRepository;
import com.fy20047.susan.repository.SheetSyncSettingsRepository;
import com.fy20047.susan.repository.SheetSyncSourceRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
    private static final String PURCHASED = "\u5df2\u63a1\u8cfc";
    private static final String DEPOSIT_PAID_DATE = "\u4ed8\u5b9a\u65e5";
    private static final String CHECKED_IN = "\u5c0d";
    private static final String SHIPPING_PROGRESS = "\u51fa\u8ca8\u9032\u5ea6";
    private static final String NOT_CHECKED_IN = "\u672a\u5831\u5230";

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

    @Test
    void syncFromCsvStoresPurchasedFlag() throws IOException {
        OrderItem item = syncSingleItem(List.of(PURCHASED), List.of("TRUE"));

        Assertions.assertEquals(true, item.getPurchased());
    }

    @Test
    void syncFromCsvUsesShippingProgressForReadyToShipStatus() throws IOException {
        OrderItem item = syncSingleItem(
                List.of(PURCHASED, CHECKED_IN, SHIPPING_PROGRESS),
                List.of("TRUE", "2026-07-01", "\u5df2\u62b5\u53f0\u5f85\u51fa\u8ca8"));

        Assertions.assertEquals(com.fy20047.susan.domain.ItemStatus.ARRIVED, item.getItemStatus());
        Assertions.assertEquals(com.fy20047.susan.domain.ShippingStatus.READY_TO_SHIP, item.getShippingStatus());
        Assertions.assertEquals(true, item.getDepositPaid());
    }

    @Test
    void syncFromCsvTreatsEitherDepositFieldAsPaid() throws IOException {
        OrderItem item = syncSingleItem(
                List.of(PURCHASED, CHECKED_IN),
                List.of("TRUE", "2026-07-01"));

        Assertions.assertEquals(com.fy20047.susan.domain.ItemStatus.IN_TRANSIT, item.getItemStatus());
        Assertions.assertEquals(true, item.getDepositPaid());
    }

    @Test
    void syncFromCsvUsesNotCheckedInForReminder() throws IOException {
        OrderItem item = syncSingleItem(
                List.of(DEPOSIT_PAID_DATE, CHECKED_IN, NOT_CHECKED_IN),
                List.of("", "2026-07-01", "TRUE"));

        Assertions.assertEquals(true, item.getCheckedIn());
    }

    @Test
    void createSyncSourceConvertsGoogleSheetEditUrlToXlsxExportUrl() {
        SheetSyncService service = syncServiceWithSourceRepositories();
        String sheetId = "1o6WQVtsjajFf2Z0mCOJKlZhOO4GfKR0y5aZhYmkucss";

        SheetSyncSource source = service.createSyncSource(
                "一般團",
                "https://docs.google.com/spreadsheets/d/" + sheetId + "/edit?gid=0#gid=0",
                GroupSourceType.STANDARD);

        Assertions.assertEquals(
                "https://docs.google.com/spreadsheets/d/" + sheetId + "/export?format=xlsx",
                source.getSheetUrl());
    }

    @Test
    void createSyncSourceExtractsGoogleSheetUrlFromMarkdownLink() {
        SheetSyncService service = syncServiceWithSourceRepositories();
        String sheetId = "1o6WQVtsjajFf2Z0mCOJKlZhOO4GfKR0y5aZhYmkucss";

        SheetSyncSource source = service.createSyncSource(
                "一般團",
                "[表單](https://docs.google.com/spreadsheets/d/" + sheetId + "/export?format=xlsx)",
                GroupSourceType.STANDARD);

        Assertions.assertEquals(
                "https://docs.google.com/spreadsheets/d/" + sheetId + "/export?format=xlsx",
                source.getSheetUrl());
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

    private SheetSyncService syncServiceWithSourceRepositories() {
        OrderGroupRepository orderGroupRepository = mock(OrderGroupRepository.class);
        SheetSyncSourceRepository sourceRepository = mock(SheetSyncSourceRepository.class);
        SheetSyncSettingsRepository settingsRepository = mock(SheetSyncSettingsRepository.class);
        SheetSyncSettings settings = new SheetSyncSettings();
        settings.setDefaultSourcesInitialized(true);

        when(settingsRepository.findById(SheetSyncSettings.SINGLETON_ID)).thenReturn(Optional.of(settings));
        when(sourceRepository.findMaxDisplayOrder()).thenReturn(0);
        when(sourceRepository.existsBySourceKey(anyString())).thenReturn(false);
        doAnswer(invocation -> invocation.getArgument(0))
                .when(sourceRepository)
                .save(any(SheetSyncSource.class));

        return new SheetSyncService(
                orderGroupRepository,
                mock(SheetSyncWriter.class),
                sourceRepository,
                settingsRepository);
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
