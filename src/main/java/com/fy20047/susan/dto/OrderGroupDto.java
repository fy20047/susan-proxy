package com.fy20047.susan.dto;

import com.fy20047.susan.domain.GroupSourceType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderGroupDto {

    private Long id;
    private String buyerNickname;
    private String groupName;
    private String sourceKey;
    private GroupSourceType sourceType;
    private LocalDateTime lastUpdated;
    private Integer totalAmount;
    private Integer totalBalance;
    private Integer bonusCount;
    private List<OrderItemDto> items = new ArrayList<>();

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

    public String getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }

    public GroupSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(GroupSourceType sourceType) {
        this.sourceType = sourceType;
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

    public Integer getBonusCount() {
        return bonusCount;
    }

    public void setBonusCount(Integer bonusCount) {
        this.bonusCount = bonusCount;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }
}
