package com.fy20047.susan.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.metadata.holder.ReadRowHolder;
import com.alibaba.excel.read.metadata.holder.ReadSheetHolder;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SheetRowListenerTest {

    @Test
    void acceptsSheetWithoutJpyPriceHeader() {
        SheetSyncWriter writer = mock(SheetSyncWriter.class);
        when(writer.createGroup(any())).thenReturn(1L);

        SheetRowListener listener = new SheetRowListener(writer, null, true, 100, 10, 64, 16);
        AnalysisContext context = mockContext("0430快閃【我英原畫展】");

        listener.invokeHeadMap(headerMapWithoutJpyPrice(), context);

        SheetRowDto row = new SheetRowDto();
        row.setBuyerNickname("Christy");
        row.setItemName("綠谷背影立牌");
        row.setDepositAmount(376);
        row.setBalanceAmount(94);
        row.setTotalAmount(470);
        row.setQuantity(1);
        row.setPurchased("TRUE");
        row.setReconciled("FALSE");
        row.setShipped("");

        listener.invoke(row, context);
        listener.doAfterAllAnalysed(context);

        verify(writer).prepareReplace("0430快閃【我英原畫展】");
        verify(writer).createGroup(any());
        verify(writer).saveItems(anyList());
    }

    @Test
    void skipsSheetWhenRequiredHeaderIsMissing() {
        SheetSyncWriter writer = mock(SheetSyncWriter.class);
        SheetRowListener listener = new SheetRowListener(writer, null, true, 100, 10, 64, 16);
        AnalysisContext context = mockContext("缺欄位測試");

        Map<Integer, String> headers = headerMapWithoutJpyPrice();
        headers.values().remove("出貨狀態");

        listener.invokeHeadMap(headers, context);

        SheetRowDto row = new SheetRowDto();
        row.setBuyerNickname("Christy");
        row.setItemName("綠谷背影立牌");
        listener.invoke(row, context);
        listener.doAfterAllAnalysed(context);

        verify(writer, never()).prepareReplace(any());
        verify(writer, never()).createGroup(any());
        verify(writer, never()).saveItems(anyList());
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
        headers.put(8, "喊單序");
        headers.put(9, "順位");
        headers.put(10, "已採購");
        headers.put(11, "品項");
        headers.put(12, "數量");
        headers.put(13, "特典");
        headers.put(14, "台幣單價");
        headers.put(15, "折扣");
        headers.put(16, "台總額");
        headers.put(17, "IP");
        headers.put(18, "未報到");
        headers.put(19, "抵台");
        headers.put(20, "出貨狀態");
        headers.put(21, "喊單日");
        headers.put(22, "備註");
        return headers;
    }
}
