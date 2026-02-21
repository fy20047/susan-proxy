package com.fy20047.susan.repository;

import com.fy20047.susan.domain.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // 依主檔 ID 取得該筆訂單的所有商品明細
    List<OrderItem> findByOrderGroupId(Long orderGroupId);

    // 依暱稱取得買家的所有商品明細（透過主檔關聯）
    List<OrderItem> findByOrderGroupBuyerNickname(String buyerNickname);

    // 彙總某買家尾款總額（不存在資料時回傳 0）
    @Query("""
            select coalesce(sum(oi.balanceAmount), 0)
            from OrderItem oi
            join oi.orderGroup og
            where og.buyerNickname = :buyerNickname
            """)
    Integer sumBalanceAmountByBuyerNickname(@Param("buyerNickname") String buyerNickname);
}
