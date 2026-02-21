package com.fy20047.susan.controller;

import com.fy20047.susan.domain.OrderGroup;
import com.fy20047.susan.domain.OrderItem;
import com.fy20047.susan.dto.OrderGroupDto;
import com.fy20047.susan.dto.OrderItemDto;
import com.fy20047.susan.repository.OrderGroupRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
// 把從倉庫拿來的 Entity 放進定義好的 DTO 裡
public class OrderQueryController {

    private final OrderGroupRepository orderGroupRepository;

    public OrderQueryController(OrderGroupRepository orderGroupRepository) {
        this.orderGroupRepository = orderGroupRepository;
    }

    // 本機驗證用：依暱稱查詢訂單與明細
    @GetMapping
    public List<OrderGroupDto> getOrdersByNickname(@RequestParam("nickname") String nickname) {
        // 去倉庫拿 Entity：這時拿出來的是帶有外鍵關聯的 OrderGroup 實體
        List<OrderGroup> groups = orderGroupRepository.findByBuyerNicknameWithItems(nickname);

        // 準備一個 List 來裝等一下整理好的資料
        List<OrderGroupDto> result = new ArrayList<>();

        // 開始處理每一筆主檔
        for (OrderGroup group : groups) {

            // 建立一個新的空白主檔
            OrderGroupDto dto = new OrderGroupDto();

            // 對接資料
            dto.setId(group.getId());
            dto.setBuyerNickname(group.getBuyerNickname());
            dto.setGroupName(group.getGroupName());
            dto.setLastUpdated(group.getLastUpdated());
            dto.setTotalAmount(group.getTotalAmount());
            dto.setTotalBalance(group.getTotalBalance());

            // 準備另一個 List 裝這個主檔底下的 OrderItem
            List<OrderItemDto> itemDtos = new ArrayList<>();

            // 處理這個主檔底下的每一份內容
            for (OrderItem item : group.getItems()) {
                OrderItemDto itemDto = new OrderItemDto();
                itemDto.setId(item.getId());
                itemDto.setOrderSn(item.getOrderSn());
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
        return result;
    }
}
