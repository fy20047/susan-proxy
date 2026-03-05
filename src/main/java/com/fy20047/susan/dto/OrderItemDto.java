package com.fy20047.susan.dto;

import com.fy20047.susan.domain.ItemStatus;

public class OrderItemDto {

    private Long id;
    private String orderSn;
    private Boolean queued;
    private Boolean checkedIn;
    private String balanceDueDate;
    private String depositPaidDate;
    private String checkMark;
    private Integer depositAmount;
    private Integer balanceAmount;
    private Integer totalAmount;
    private String itemName;
    private Integer quantity;
    private Integer jpyPrice;
    private ItemStatus itemStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderSn() {
        return orderSn;
    }

    public void setOrderSn(String orderSn) {
        this.orderSn = orderSn;
    }

    public Boolean getQueued() {
        return queued;
    }

    public void setQueued(Boolean queued) {
        this.queued = queued;
    }

    public Boolean getCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(Boolean checkedIn) {
        this.checkedIn = checkedIn;
    }

    public String getBalanceDueDate() {
        return balanceDueDate;
    }

    public void setBalanceDueDate(String balanceDueDate) {
        this.balanceDueDate = balanceDueDate;
    }

    public String getDepositPaidDate() {
        return depositPaidDate;
    }

    public void setDepositPaidDate(String depositPaidDate) {
        this.depositPaidDate = depositPaidDate;
    }

    public String getCheckMark() {
        return checkMark;
    }

    public void setCheckMark(String checkMark) {
        this.checkMark = checkMark;
    }

    public Integer getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(Integer depositAmount) {
        this.depositAmount = depositAmount;
    }

    public Integer getBalanceAmount() {
        return balanceAmount;
    }

    public void setBalanceAmount(Integer balanceAmount) {
        this.balanceAmount = balanceAmount;
    }

    public Integer getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Integer totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getJpyPrice() {
        return jpyPrice;
    }

    public void setJpyPrice(Integer jpyPrice) {
        this.jpyPrice = jpyPrice;
    }

    public ItemStatus getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(ItemStatus itemStatus) {
        this.itemStatus = itemStatus;
    }
}
