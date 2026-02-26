package com.fy20047.susan.service;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.fy20047.susan.domain.ItemStatus;
import com.fy20047.susan.domain.OrderGroup;
import com.fy20047.susan.domain.OrderItem;
import com.fy20047.susan.repository.OrderGroupRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SheetRowListener extends AnalysisEventListener<SheetRowDto> {

    private static final Logger log = LoggerFactory.getLogger(SheetRowListener.class);

    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "對帳", "定金80%", "尾款20%", "購買總額", "團友", "品項", "日幣原價", "已採購", "出貨狀態"
    );

    private static final Set<String> TRUE_VALUES = Set.of("TRUE", "T", "1", "Y", "YES", "V");

    private final OrderGroupRepository orderGroupRepository;
    private final Map<String, OrderGroup> groupByBuyer = new LinkedHashMap<>();
    private String currentSheetName = "";
    private boolean validSheet = false;

    public SheetRowListener(OrderGroupRepository orderGroupRepository) {
        this.orderGroupRepository = orderGroupRepository;
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        currentSheetName = context.readSheetHolder().getSheetName();
        groupByBuyer.clear();
        validSheet = false;

        if (headMap == null || headMap.isEmpty()) {
            log.warn("分頁 {} 找不到欄位表頭，將跳過。", currentSheetName);
            return;
        }

        Set<String> headers = new HashSet<>();
        for (String header : headMap.values()) {
            if (header != null && !header.trim().isEmpty()) {
                headers.add(header.trim());
            }
        }
        if (!headers.containsAll(REQUIRED_HEADERS)) {
            log.warn("分頁 {} 欄位表頭不完整，將跳過。實際欄位: {}", currentSheetName, headers);
            return;
        }
        validSheet = true;
    }

    @Override
    public void invoke(SheetRowDto row, AnalysisContext context) {
        if (!validSheet) {
            return;
        }

        String buyerNickname = safeString(row.getBuyerNickname());
        String itemName = safeString(row.getItemName());

        if (buyerNickname.isEmpty() || itemName.isEmpty()) {
            return;
        }

        OrderGroup group = groupByBuyer.computeIfAbsent(buyerNickname, key -> {
            OrderGroup newGroup = new OrderGroup();
            newGroup.setBuyerNickname(key);
            newGroup.setGroupName(currentSheetName);
            newGroup.setLastUpdated(LocalDateTime.now());
            return newGroup;
        });

        OrderItem item = new OrderItem();
        String orderSn = safeString(row.getOrderRank());
        if (orderSn.isEmpty()) {
            orderSn = safeString(row.getOrderSn());
        }
        item.setOrderSn(orderSn);
        item.setQueued(parseBoolean(row.getQueued()));
        item.setCheckedIn(parseBoolean(row.getCheckedIn()));
        item.setBalanceDueDate(safeString(row.getBalanceDueDate()));
        item.setDepositPaidDate(safeString(row.getDepositPaidDate()));
        item.setDepositAmount(defaultInt(row.getDepositAmount()));
        item.setBalanceAmount(defaultInt(row.getBalanceAmount()));
        item.setTotalAmount(defaultInt(row.getTotalAmount()));
        item.setItemName(itemName);
        item.setQuantity(defaultInt(row.getQuantity(), 1));
        item.setJpyPrice(row.getJpyPrice());

        boolean isReconciled = parseBoolean(row.getReconciled());
        boolean isPurchased = parseBoolean(row.getPurchased());
        boolean isShipped = parseBoolean(row.getShipped());
        item.setItemStatus(determineStatus(isReconciled, isPurchased, false, isShipped));

        group.addItem(item);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        if (!validSheet || groupByBuyer.isEmpty()) {
            return;
        }

        List<OrderGroup> existingGroups = orderGroupRepository.findByGroupName(currentSheetName);
        if (!existingGroups.isEmpty()) {
            orderGroupRepository.deleteAll(existingGroups);
        }
        orderGroupRepository.saveAll(groupByBuyer.values());
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean parseBoolean(String rawValue) {
        if (rawValue == null) {
            return false;
        }
        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return false;
        }
        return TRUE_VALUES.contains(normalized);
    }

    private int defaultInt(Integer value) {
        return defaultInt(value, 0);
    }

    private int defaultInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private ItemStatus determineStatus(boolean isReconciled, boolean isPurchased, boolean isArrived, boolean isShipped) {
        if (isShipped) {
            return ItemStatus.SHIPPED;
        }
        if (isArrived) {
            return ItemStatus.ARRIVED;
        }

        if (isPurchased && isReconciled) {
            return ItemStatus.IN_TRANSIT;
        } else if (isPurchased && !isReconciled) {
            return ItemStatus.PENDING_DEPOSIT;
        } else if (!isPurchased && isReconciled) {
            return ItemStatus.PENDING_PURCHASE;
        } else {
            return ItemStatus.REGISTERED;
        }
    }
}
