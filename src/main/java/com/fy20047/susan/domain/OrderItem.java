package com.fy20047.susan.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "order_item",
        indexes = {
                @Index(name = "idx_order_item_group_id", columnList = "order_group_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "orderGroup")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 關聯至訂單主檔
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_group_id", nullable = false)
    private OrderGroup orderGroup;

    // 順位（CSV 原始欄位）
    @Column(name = "order_sn", length = 64)
    private String orderSn;

    // 是否排到（勾選代表已排到）
    @Column(name = "is_queued")
    private Boolean queued = false;

    // 尾款日（CSV 可能不是純日期，先以字串保存）
    @Column(name = "balance_due_date", length = 32)
    private String balanceDueDate;

    // 付定日（CSV 可能不是純日期，先以字串保存）
    @Column(name = "deposit_paid_date", length = 32)
    private String depositPaidDate;

    // 定金80%
    @Column(name = "deposit_amount")
    private Integer depositAmount;

    // 尾款20%
    @Column(name = "balance_amount")
    private Integer balanceAmount;

    // 購買總額
    @Column(name = "total_amount")
    private Integer totalAmount;

    // 品項
    @Column(name = "item_name", length = 255, nullable = false)
    private String itemName;

    // 購買數量（透過 Service 從品名中自動萃取，保證至少為 1）
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    // 日幣原價
    @Column(name = "jpy_price")
    private Integer jpyPrice;

    // 商品狀態碼（由 CSV 狀態欄位轉換而來）
    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", length = 32)
    private ItemStatus itemStatus;
}
