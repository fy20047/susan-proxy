package com.fy20047.susan.repository;

import com.fy20047.susan.domain.SheetSyncSource;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SheetSyncSourceRepository extends JpaRepository<SheetSyncSource, Long> {

    List<SheetSyncSource> findAllByOrderByDisplayOrderAscIdAsc();

    boolean existsBySourceKey(String sourceKey);

    @Query("select coalesce(max(s.displayOrder), 0) from SheetSyncSource s")
    int findMaxDisplayOrder();

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SheetSyncSource s
            set s.lastSyncedAt = :syncedAt,
                s.updatedAt = :syncedAt
            where s.id in :sourceIds
            """)
    int updateLastSyncedAt(
            @Param("sourceIds") Collection<Long> sourceIds,
            @Param("syncedAt") LocalDateTime syncedAt
    );
}
