package com.fy20047.susan.controller;

import com.fy20047.susan.domain.OrderGroup;
import com.fy20047.susan.domain.OrderItem;
import com.fy20047.susan.dto.ApiResponse;
import com.fy20047.susan.dto.OrderGroupDto;
import com.fy20047.susan.dto.OrderItemDto;
import com.fy20047.susan.repository.OrderGroupRepository;
import java.util.ArrayList;
import java.util.List;
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
                    .body(ApiResponse.error("INVALID_REQUEST", "nickname 不能為空"));
        }

        List<OrderGroup> groups = orderGroupRepository.findByBuyerNicknameWithItems(normalized);
        if (groups.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", "查無相關訂單"));
        }

        // 以大小寫完全相同為準，避免資料庫預設不分大小寫
        List<OrderGroup> exactGroups = new ArrayList<>();
        for (OrderGroup group : groups) {
            if (normalized.equals(group.getBuyerNickname())) {
                exactGroups.add(group);
            }
        }
        if (exactGroups.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", "查無相關訂單"));
        }

        List<OrderGroupDto> result = new ArrayList<>();
        for (OrderGroup group : exactGroups) {
            OrderGroupDto dto = new OrderGroupDto();
            dto.setId(group.getId());
            dto.setBuyerNickname(group.getBuyerNickname());
            dto.setGroupName(group.getGroupName());
            dto.setLastUpdated(group.getLastUpdated());
            dto.setTotalAmount(group.getTotalAmount());
            dto.setTotalBalance(group.getTotalBalance());

            List<OrderItemDto> itemDtos = new ArrayList<>();
            for (OrderItem item : group.getItems()) {
                OrderItemDto itemDto = new OrderItemDto();
                itemDto.setId(item.getId());
                itemDto.setOrderSn(item.getOrderSn());
                itemDto.setQueued(item.getQueued());
                itemDto.setCheckedIn(item.getCheckedIn());
                itemDto.setBalanceDueDate(item.getBalanceDueDate());
                itemDto.setDepositPaidDate(item.getDepositPaidDate());
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
}
