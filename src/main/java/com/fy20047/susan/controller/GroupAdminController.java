package com.fy20047.susan.controller;

import com.fy20047.susan.domain.OrderGroup;
import com.fy20047.susan.dto.ApiResponse;
import com.fy20047.susan.repository.OrderGroupRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class GroupAdminController {

    private final OrderGroupRepository orderGroupRepository;

    public GroupAdminController(OrderGroupRepository orderGroupRepository) {
        this.orderGroupRepository = orderGroupRepository;
    }

    // 刪除指定團名的所有訂單主檔與明細
    @DeleteMapping("/groups")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteGroup(
            @RequestParam("groupName") String groupName
    ) {
        String normalized = groupName == null ? "" : groupName.trim();
        if (normalized.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_REQUEST", "groupName 不能為空"));
        }

        List<OrderGroup> groups = orderGroupRepository.findByGroupName(normalized);
        if (groups.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", "查無指定團名資料"));
        }

        orderGroupRepository.deleteAll(groups);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupName", normalized);
        result.put("deletedCount", groups.size());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
