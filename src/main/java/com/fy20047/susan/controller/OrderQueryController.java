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

    // 依暱稱查詢訂單與明細（回傳 DTO）
    @GetMapping
    // 控制 HTTP 的狀態碼，讓前端瀏覽器的 Network 能抓到狀態，再加上 ApiResponse讓 Body 裡面放統一的 JSON 格式
    public ResponseEntity<ApiResponse<List<OrderGroupDto>>> getOrdersByNickname(
            @RequestParam("nickname") String nickname
    ) {
        String normalized = nickname == null ? "" : nickname.trim();
        if (normalized.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_REQUEST", "nickname 不能為空")); // nickname 空值回 400
        }

        List<OrderGroup> groups = orderGroupRepository.findByBuyerNicknameWithItems(normalized);
        if (groups.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", "查無符合的訂單資料")); // 查無資料回 404
        }

        List<OrderGroupDto> result = new ArrayList<>();
        for (OrderGroup group : groups) {
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

        return ResponseEntity.ok(ApiResponse.success(result)); // 成功回 ApiResponse 包裝的 DTO
    }
}
