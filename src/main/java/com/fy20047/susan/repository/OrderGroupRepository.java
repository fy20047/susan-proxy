package com.fy20047.susan.repository;

import com.fy20047.susan.domain.OrderGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderGroupRepository extends JpaRepository<OrderGroup, Long> {

    @Query("select distinct og from OrderGroup og left join fetch og.items where og.buyerNickname = :buyerNickname")
    List<OrderGroup> findByBuyerNicknameWithItems(@Param("buyerNickname") String buyerNickname);

    List<OrderGroup> findByGroupName(String groupName);

    List<OrderGroup> findByGroupNameAndSourceKey(String groupName, String sourceKey);

    List<OrderGroup> findBySourceKey(String sourceKey);
}
