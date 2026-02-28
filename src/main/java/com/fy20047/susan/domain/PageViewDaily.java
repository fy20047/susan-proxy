package com.fy20047.susan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "page_view_daily")
@Getter
@Setter
@NoArgsConstructor
public class PageViewDaily {

    // 每日日期（主鍵）
    @Id
    @Column(name = "view_date", nullable = false)
    private LocalDate viewDate;

    // 當日瀏覽量
    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;
}
