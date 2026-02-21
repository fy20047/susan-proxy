package com.fy20047.susan.domain;

// 商品狀態列舉，用於對應 CSV 狀態欄位的轉換結果
public enum ItemStatus {
    // 已登記：有品項名稱，未採購未對帳
    REGISTERED,
    // 待匯定：已採購，未對帳
    PENDING_DEPOSIT,
    // 待購入：已對帳，未採購
    PENDING_PURCHASE,
    // 運送中：已對帳，已採購，未抵台
    IN_TRANSIT,
    // 已抵台：已對帳，已採購，已抵台，未出貨
    ARRIVED,
    // 已出貨：已出貨完成
    SHIPPED
}
