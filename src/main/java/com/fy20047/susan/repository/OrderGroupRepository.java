package com.fy20047.susan.repository;

import com.fy20047.susan.domain.OrderGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderGroupRepository extends JpaRepository<OrderGroup, Long> {

    // 依暱稱查詢買家所有訂單主檔
    List<OrderGroup> findByBuyerNickname(String buyerNickname);

    // 依團名查詢所有訂單主檔
    List<OrderGroup> findByGroupName(String groupName);
}
