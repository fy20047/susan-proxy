package com.fy20047.susan.repository;

import com.fy20047.susan.domain.OrderGroup;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OrderGroupRepository extends JpaRepository<OrderGroup, Long> {

    @Query("select distinct og from OrderGroup og left join fetch og.items where og.buyerNickname = :buyerNickname")
    List<OrderGroup> findByBuyerNicknameWithItems(@Param("buyerNickname") String buyerNickname);

    List<OrderGroup> findByGroupName(String groupName);

    List<OrderGroup> findByGroupNameAndSourceKey(String groupName, String sourceKey);

    @Query("""
            select og from OrderGroup og
            where og.groupName = :groupName
              and (
                  og.sourceKey = :sourceKey
                  or og.sourceKey is null
                  or trim(og.sourceKey) = ''
              )
            """)
    List<OrderGroup> findByGroupNameAndSourceKeyIncludingLegacy(
            @Param("groupName") String groupName,
            @Param("sourceKey") String sourceKey
    );

    List<OrderGroup> findBySourceKey(String sourceKey);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OrderGroup og
            set og.lastUpdated = :completedAt
            where og.sourceKey in :sourceKeys
              and og.lastUpdated = :syncTimestamp
            """)
    int updateLastUpdatedForSyncedSources(
            @Param("sourceKeys") Collection<String> sourceKeys,
            @Param("syncTimestamp") LocalDateTime syncTimestamp,
            @Param("completedAt") LocalDateTime completedAt
    );
}
