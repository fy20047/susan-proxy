package com.fy20047.susan.controller;

import com.fy20047.susan.domain.OrderGroup;
import com.fy20047.susan.domain.OrderItem;
import com.fy20047.susan.dto.ApiResponse;
import com.fy20047.susan.dto.OrderGroupDto;
import com.fy20047.susan.dto.OrderItemDto;
import com.fy20047.susan.repository.OrderGroupRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderQueryController {

    private final OrderGroupRepository orderGroupRepository;

    public OrderQueryController(OrderGroupRepository orderGroupRepository) {
        this.orderGroupRepository = orderGroupRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderGroupDto>>> getOrdersByNickname(
            @RequestParam("nickname") String nickname
    ) {
        String normalized = nickname == null ? "" : nickname.trim();
        if (normalized.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_REQUEST", "nickname is required."));
        }

        List<OrderGroup> groups = orderGroupRepository.findByBuyerNicknameWithItems(normalized);
        if (groups.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", "查無符合的訂單資料。"));
        }

        List<OrderGroup> exactGroups = new ArrayList<>();
        for (OrderGroup group : groups) {
            if (normalized.equals(group.getBuyerNickname())) {
                exactGroups.add(group);
            }
        }
        if (exactGroups.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", "查無符合的訂單資料。"));
        }

        List<OrderGroupDto> result = new ArrayList<>();
        for (OrderGroup group : collapseLegacyDuplicates(exactGroups)) {
            OrderGroupDto dto = new OrderGroupDto();
            dto.setId(group.getId());
            dto.setBuyerNickname(group.getBuyerNickname());
            dto.setGroupName(group.getGroupName());
            dto.setSourceKey(group.getSourceKey());
            dto.setSourceType(group.getSourceType());
            dto.setLastUpdated(group.getLastUpdated());
            dto.setTotalAmount(group.getTotalAmount());
            dto.setTotalBalance(group.getTotalBalance());
            dto.setBonusCount(group.getBonusCount());

            List<OrderItemDto> itemDtos = new ArrayList<>();
            for (OrderItem item : group.getItems()) {
                OrderItemDto itemDto = new OrderItemDto();
                itemDto.setId(item.getId());
                itemDto.setOrderSn(item.getOrderSn());
                itemDto.setQueued(item.getQueued());
                itemDto.setCheckedIn(item.getCheckedIn());
                itemDto.setBalanceDueDate(item.getBalanceDueDate());
                itemDto.setDepositPaidDate(item.getDepositPaidDate());
                itemDto.setDepositReconciled(item.getDepositReconciled());
                itemDto.setPurchased(item.getPurchased());
                itemDto.setCheckMark(item.getCheckMark());
                itemDto.setDepositAmount(item.getDepositAmount());
                itemDto.setBalanceAmount(item.getBalanceAmount());
                itemDto.setTotalAmount(item.getTotalAmount());
                itemDto.setItemName(item.getItemName());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setJpyPrice(item.getJpyPrice());
                itemDto.setItemStatus(item.getItemStatus());
                itemDtos.add(itemDto);
            }
            dto.setItems(itemDtos);
            result.add(dto);
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private List<OrderGroup> collapseLegacyDuplicates(List<OrderGroup> groups) {
        Map<String, OrderGroup> byDisplayKey = new LinkedHashMap<>();
        for (OrderGroup group : groups) {
            String key = buildDisplayKey(group);
            OrderGroup existing = byDisplayKey.get(key);
            if (existing == null || shouldReplaceLegacy(existing, group)) {
                byDisplayKey.put(key, group);
            }
        }
        return new ArrayList<>(byDisplayKey.values());
    }

    private String buildDisplayKey(OrderGroup group) {
        String buyerNickname = group.getBuyerNickname() == null ? "" : group.getBuyerNickname().trim();
        String groupName = group.getGroupName() == null ? "" : group.getGroupName().trim();
        String sourceType = group.getSourceType() == null ? "" : group.getSourceType().name();
        return buyerNickname + "\n" + groupName + "\n" + sourceType;
    }

    private boolean shouldReplaceLegacy(OrderGroup existing, OrderGroup candidate) {
        boolean existingLegacy = isLegacySource(existing);
        boolean candidateLegacy = isLegacySource(candidate);
        if (existingLegacy != candidateLegacy) {
            return existingLegacy;
        }
        if (existing.getLastUpdated() == null) {
            return candidate.getLastUpdated() != null;
        }
        if (candidate.getLastUpdated() == null) {
            return false;
        }
        return candidate.getLastUpdated().isAfter(existing.getLastUpdated());
    }

    private boolean isLegacySource(OrderGroup group) {
        String sourceKey = group.getSourceKey();
        return sourceKey == null || sourceKey.trim().isEmpty();
    }
}
