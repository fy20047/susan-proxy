package com.fy20047.susan.service;

import com.fy20047.susan.dto.PageViewStatsDto;
import com.fy20047.susan.repository.PageViewDailyRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PageViewService {

    private static final ZoneId TAIPEI_ZONE = ZoneId.of("Asia/Taipei");

    private final PageViewDailyRepository pageViewDailyRepository;

    public PageViewService(PageViewDailyRepository pageViewDailyRepository) {
        this.pageViewDailyRepository = pageViewDailyRepository;
    }

    @Transactional
    public PageViewStatsDto recordVisitAndGetStats() {
        LocalDate today = LocalDate.now(TAIPEI_ZONE);
        pageViewDailyRepository.incrementDaily(today);
        return getStats(today);
    }

    @Transactional(readOnly = true)
    public PageViewStatsDto getStats() {
        LocalDate today = LocalDate.now(TAIPEI_ZONE);
        return getStats(today);
    }

    private PageViewStatsDto getStats(LocalDate today) {
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate nextMonthStart = monthStart.plusMonths(1);

        Long daily = pageViewDailyRepository.findCountByDate(today);
        Long monthly = pageViewDailyRepository.sumByDateRange(monthStart, nextMonthStart);
        Long total = pageViewDailyRepository.sumTotal();

        PageViewStatsDto dto = new PageViewStatsDto();
        dto.setDaily(daily == null ? 0 : daily);
        dto.setMonthly(monthly == null ? 0 : monthly);
        dto.setTotal(total == null ? 0 : total);
        return dto;
    }
}
