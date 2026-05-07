package com.fy20047.susan.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Column(name = "buyer_nickname", length = 128, nullable = false)
    private String buyerNickname;

    @Column(name = "group_name", length = 128)
    private String groupName;

    @Column(name = "source_key", length = 128)
    private String sourceKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 32)
    private GroupSourceType sourceType = GroupSourceType.STANDARD;

    @Column(name = "bonus_count")
    private Integer bonusCount;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @OneToMany(mappedBy = "orderGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrderGroup(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrderGroup(null);
    }

    @Transient
    public Integer getTotalAmount() {
        if (items == null) {
            return 0;
        }
        return items.stream()
                .mapToInt(item -> item.getTotalAmount() == null ? 0 : item.getTotalAmount())
                .sum();
    }

    @Transient
    public Integer getTotalBalance() {
        if (items == null) {
            return 0;
        }
        return items.stream()
                .mapToInt(item -> item.getBalanceAmount() == null ? 0 : item.getBalanceAmount())
                .sum();
    }
}
