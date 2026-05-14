package com.fy20047.susan.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.metadata.holder.ReadRowHolder;
import com.alibaba.excel.read.metadata.holder.ReadSheetHolder;
import com.fy20047.susan.domain.GroupSourceType;
import com.fy20047.susan.domain.OrderItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SheetRowListenerTest {

    @Test
    void acceptsSheetWithoutJpyPriceHeader() {
        SheetSyncWriter writer = mock(SheetSyncWriter.class);
        when(writer.createGroup(any())).thenReturn(1L);
        List<OrderItem> savedItems = new java.util.ArrayList<>();
        doAnswer(invocation -> {
            savedItems.addAll(invocation.getArgument(0));
            return null;
        }).when(writer).saveItems(anyList());

        SheetRowListener listener = new SheetRowListener(
                writer,
                "standard",
                GroupSourceType.STANDARD,
                null,
                true,
                100,
                10,
                64,
                16);
        AnalysisContext context = mockContext("0430測試團");

        listener.invokeHeadMap(headerMapWithoutJpyPrice(), context);

        SheetRowDto row = new SheetRowDto();
        row.setBuyerNickname("Christy");
        row.setItemName("測試商品");
        row.setDepositAmount(376);
        row.setBalanceAmount(94);
        row.setTotalAmount(470);
        row.setQuantity(1);
        row.setPurchased("TRUE");
        row.setReconciled("FALSE");
        row.setShipped("");

        listener.invoke(row, context);
        listener.doAfterAllAnalysed(context);

        verify(writer).prepareReplace("0430測試團", "standard");
        verify(writer).createGroup(any());
        verify(writer).saveItems(anyList());
        Assertions.assertEquals(false, savedItems.getFirst().getDepositReconciled());
    }

    @Test
    void storesDepositReconciledFromReconciledCheckbox() {
        SheetSyncWriter writer = mock(SheetSyncWriter.class);
        when(writer.createGroup(any())).thenReturn(1L);
        List<OrderItem> savedItems = new java.util.ArrayList<>();
        doAnswer(invocation -> {
            savedItems.addAll(invocation.getArgument(0));
            return null;
        }).when(writer).saveItems(anyList());

        SheetRowListener listener = new SheetRowListener(
                writer,
                "standard",
                GroupSourceType.STANDARD,
                null,
                true,
                100,
                10,
                64,
                16);
        AnalysisContext context = mockContext("0330測試團");

        listener.invokeHeadMap(headerMapWithoutJpyPrice(), context);

        SheetRowDto row = new SheetRowDto();
        row.setBuyerNickname("002");
        row.setItemName("金屬卡 - 單抽");
        row.setDepositPaidDate("2026-04-01");
        row.setReconciled("TRUE");
        row.setDepositAmount(620);
        row.setBalanceAmount(155);
        row.setTotalAmount(775);
        row.setQuantity(5);

        listener.invoke(row, context);
        listener.doAfterAllAnalysed(context);

        verify(writer).saveItems(anyList());
        Assertions.assertEquals(true, savedItems.getFirst().getDepositReconciled());
    }

    @Test
    void skipsSheetWhenRequiredHeaderIsMissing() {
        SheetSyncWriter writer = mock(SheetSyncWriter.class);
        SheetRowListener listener = new SheetRowListener(writer, null, true, 100, 10, 64, 16);
        AnalysisContext context = mockContext("缺欄位分頁");

        Map<Integer, String> headers = headerMapWithoutJpyPrice();
        headers.values().remove("對帳");

        listener.invokeHeadMap(headers, context);

        SheetRowDto row = new SheetRowDto();
        row.setBuyerNickname("Christy");
        row.setItemName("測試商品");
        listener.invoke(row, context);
        listener.doAfterAllAnalysed(context);

        verify(writer, never()).prepareReplace(any(), any());
        verify(writer, never()).createGroup(any());
        verify(writer, never()).saveItems(anyList());
    }

    @Test
    void usesPreorderStatusWhenConfigured() {
        SheetSyncWriter writer = mock(SheetSyncWriter.class);
        when(writer.createGroup(any())).thenReturn(1L);

        Map<String, SheetSyncSheetConfig> sheetConfigs = new LinkedHashMap<>();
        sheetConfigs.put("受注測試團", new SheetSyncSheetConfig(true, true));

        SheetRowListener listener = new SheetRowListener(
                writer,
                "preorder",
                GroupSourceType.PREORDER,
                sheetConfigs,
                true,
                100,
                10,
                64,
                16);
        AnalysisContext context = mockContext("受注測試團");

        listener.invokeHeadMap(preorderHeaderMap(), context);

        SheetRowDto row = new SheetRowDto();
        row.setBuyerNickname("Buyer");
        row.setItemName("商品");
        row.setDepositAmount(100);
        row.setBalanceAmount(50);
        row.setTotalAmount(150);
        row.setQuantity(1);
        row.setPreorderStatus("已抵台");

        listener.invoke(row, context);
        listener.doAfterAllAnalysed(context);

        verify(writer).prepareReplace("受注測試團", "preorder");
        verify(writer).createGroup(any());
        verify(writer).saveItems(anyList());
    }

    private static AnalysisContext mockContext(String sheetName) {
        AnalysisContext context = mock(AnalysisContext.class);
        ReadSheetHolder readSheetHolder = mock(ReadSheetHolder.class);
        ReadRowHolder readRowHolder = mock(ReadRowHolder.class);
        when(context.readSheetHolder()).thenReturn(readSheetHolder);
        when(readSheetHolder.getSheetName()).thenReturn(sheetName);
        when(context.readRowHolder()).thenReturn(readRowHolder);
        when(readRowHolder.getRowIndex()).thenReturn(1);
        return context;
    }

    private static Map<Integer, String> headerMapWithoutJpyPrice() {
        Map<Integer, String> headers = new LinkedHashMap<>();
        headers.put(0, "尾款日");
        headers.put(1, "付定日");
        headers.put(2, "對");
        headers.put(3, "對帳");
        headers.put(4, "定金80%");
        headers.put(5, "尾款20%");
        headers.put(6, "購買總額");
        headers.put(7, "團友");
        headers.put(8, "順位");
        headers.put(9, "喊單序");
        headers.put(10, "已採購");
        headers.put(11, "品項");
        headers.put(12, "數量");
        headers.put(13, "特典");
        headers.put(14, "台幣單價");
        headers.put(15, "購買地點");
        headers.put(16, "備註");
        headers.put(17, "IP");
        headers.put(18, "抵台");
        headers.put(19, "出貨狀態");
        headers.put(20, "喊單日");
        return headers;
    }

    private static Map<Integer, String> preorderHeaderMap() {
        Map<Integer, String> headers = headerMapWithoutJpyPrice();
        headers.put(13, "貨況");
        return headers;
    }
}
