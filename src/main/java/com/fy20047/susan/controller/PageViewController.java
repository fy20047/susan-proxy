package com.fy20047.susan.controller;

import com.fy20047.susan.dto.ApiResponse;
import com.fy20047.susan.dto.PageViewStatsDto;
import com.fy20047.susan.service.PageViewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pv")
public class PageViewController {

    private final PageViewService pageViewService;

    public PageViewController(PageViewService pageViewService) {
        this.pageViewService = pageViewService;
    }

    @PostMapping("/visit")
    public ResponseEntity<ApiResponse<PageViewStatsDto>> recordVisit() {
        PageViewStatsDto stats = pageViewService.recordVisitAndGetStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageViewStatsDto>> getStats() {
        PageViewStatsDto stats = pageViewService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
