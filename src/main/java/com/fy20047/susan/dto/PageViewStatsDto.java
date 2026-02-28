package com.fy20047.susan.dto;

public class PageViewStatsDto {

    private Long daily;
    private Long monthly;
    private Long total;

    public Long getDaily() {
        return daily;
    }

    public void setDaily(Long daily) {
        this.daily = daily;
    }

    public Long getMonthly() {
        return monthly;
    }

    public void setMonthly(Long monthly) {
        this.monthly = monthly;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
