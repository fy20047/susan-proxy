package com.fy20047.susan.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "order_group",
        indexes = {
                @Index(name = "idx_order_group_buyer_nickname", columnList = "buyer_nickname")
        }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "items")
public class OrderGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 買家社群暱稱（查詢憑證）
    @Column(name = "buyer_nickname", length = 128, nullable = false)
    private String buyerNickname;

    // 連線梯次／團名
    @Column(name = "group_name", length = 128)
    private String groupName;

    // 訂單特典張數
    @Column(name = "bonus_count")
    private Integer bonusCount;

    // 資料最後同步時間
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // 一筆主檔包含多筆商品明細
    @OneToMany(mappedBy = "orderGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // 維持雙向關聯一致性（避免只加到 List 但未設定外鍵）
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrderGroup(this);
    }

    // 維持雙向關聯一致性（移除時同步清空外鍵）
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrderGroup(null);
    }

    // 動態計算整單總額 (不存入資料庫)
    @Transient
    public Integer getTotalAmount() {
        if (items == null) return 0;
        return items.stream()
                .mapToInt(item -> item.getTotalAmount() == null ? 0 : item.getTotalAmount())
                .sum();
    }

    // 動態計算整單應付尾款
    @Transient
    public Integer getTotalBalance() {
        if (items == null) return 0;
        return items.stream()
                .mapToInt(item -> item.getBalanceAmount() == null ? 0 : item.getBalanceAmount())
                .sum();
    }
}
