package com.fy20047.susan.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fy20047.susan.domain.GroupSourceType;
import com.fy20047.susan.domain.OrderGroup;
import com.fy20047.susan.dto.ApiResponse;
import com.fy20047.susan.dto.OrderGroupDto;
import com.fy20047.susan.repository.OrderGroupRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class OrderQueryControllerTest {

    @Test
    void prefersSourceKeyRecordWhenLegacyDuplicateExists() {
        OrderGroupRepository repository = mock(OrderGroupRepository.class);
        OrderQueryController controller = new OrderQueryController(repository);
        String buyerNickname = "buyer-99";
        String duplicatedGroupName = "group-attack";
        String anotherGroupName = "group-osaka";

        OrderGroup legacy = buildGroup(1L, buyerNickname, duplicatedGroupName, null,
                GroupSourceType.STANDARD, LocalDateTime.of(2026, 5, 7, 18, 0));
        OrderGroup current = buildGroup(2L, buyerNickname, duplicatedGroupName, "standard",
                GroupSourceType.STANDARD, LocalDateTime.of(2026, 5, 8, 10, 0));
        OrderGroup other = buildGroup(3L, buyerNickname, anotherGroupName, "standard",
                GroupSourceType.STANDARD, LocalDateTime.of(2026, 5, 8, 10, 0));

        when(repository.findByBuyerNicknameWithItems(buyerNickname)).thenReturn(List.of(legacy, current, other));

        ResponseEntity<ApiResponse<List<OrderGroupDto>>> response = controller.getOrdersByNickname(buyerNickname);

        ApiResponse<List<OrderGroupDto>> body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getData());
        assertEquals(2, body.getData().size());
        assertEquals(List.of(duplicatedGroupName, anotherGroupName),
                body.getData().stream().map(OrderGroupDto::getGroupName).toList());
        assertEquals(List.of("standard", "standard"),
                body.getData().stream().map(OrderGroupDto::getSourceKey).toList());
    }

    private OrderGroup buildGroup(
            Long id,
            String buyerNickname,
            String groupName,
            String sourceKey,
            GroupSourceType sourceType,
            LocalDateTime lastUpdated
    ) {
        OrderGroup group = new OrderGroup();
        group.setId(id);
        group.setBuyerNickname(buyerNickname);
        group.setGroupName(groupName);
        group.setSourceKey(sourceKey);
        group.setSourceType(sourceType);
        group.setLastUpdated(lastUpdated);
        return group;
    }
}
