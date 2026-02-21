package com.fy20047.susan.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 資料傳輸物件 (DTO) 只負責裝資料，不負責跟資料庫連線。
public class OrderGroupDto {

    private Long id;
    private String buyerNickname;
    private String groupName;
    private LocalDateTime lastUpdated;
    private Integer totalAmount;
    private Integer totalBalance;
    private List<OrderItemDto> items = new ArrayList<>(); // 裝的是 OrderItemDto 而不是 Entity

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBuyerNickname() {
        return buyerNickname;
    }

    public void setBuyerNickname(String buyerNickname) {
        this.buyerNickname = buyerNickname;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Integer getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Integer totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(Integer totalBalance) {
        this.totalBalance = totalBalance;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }
}
