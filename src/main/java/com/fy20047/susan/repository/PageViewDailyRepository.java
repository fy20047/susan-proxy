package com.fy20047.susan.repository;

import com.fy20047.susan.domain.PageViewDaily;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PageViewDailyRepository extends JpaRepository<PageViewDaily, LocalDate> {

    @Modifying
    @Query(
            value = "INSERT INTO page_view_daily (view_date, view_count) VALUES (:date, 1) " +
                    "ON DUPLICATE KEY UPDATE view_count = view_count + 1",
            nativeQuery = true
    )
    void incrementDaily(@Param("date") LocalDate date);

    @Query("SELECT p.viewCount FROM PageViewDaily p WHERE p.viewDate = :date")
    Long findCountByDate(@Param("date") LocalDate date);

    @Query(
            value = "SELECT COALESCE(SUM(view_count), 0) FROM page_view_daily " +
                    "WHERE view_date >= :startDate AND view_date < :endDate",
            nativeQuery = true
    )
    Long sumByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT COALESCE(SUM(view_count), 0) FROM page_view_daily", nativeQuery = true)
    Long sumTotal();
}
