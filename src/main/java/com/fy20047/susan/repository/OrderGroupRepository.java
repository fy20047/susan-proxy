package com.fy20047.susan.repository;

import com.fy20047.susan.domain.OrderGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderGroupRepository extends JpaRepository<OrderGroup, Long> {

    // 依暱稱查詢買家所有訂單主檔
    // JPQL (Java Persistence Query Language)
    // select distinct og : 過濾掉重複的主檔 (去除重複，只留唯一，解決笛卡兒積重複問題)
    // from OrderGroup og : 從 OrderGroup 抓資料
    // left join fetch og.items : 把掛在上面的 items 一次全部抓 (fetch)」回來(解決 N+1 效能問題)
    // where og.buyerNickname = :buyerNickname : 篩選條件是買家暱稱
    @Query("select distinct og from OrderGroup og left join fetch og.items where og.buyerNickname = :buyerNickname")
    List<OrderGroup> findByBuyerNicknameWithItems(@Param("buyerNickname") String buyerNickname);

    // 依團名查詢所有訂單主檔
    List<OrderGroup> findByGroupName(String groupName);
}
