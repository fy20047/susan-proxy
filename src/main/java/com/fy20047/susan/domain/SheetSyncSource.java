package com.fy20047.susan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "sheet_sync_source",
        indexes = {
                @Index(name = "idx_sheet_sync_source_key", columnList = "source_key", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
public class SheetSyncSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", length = 128, nullable = false)
    private String displayName;

    @Column(name = "source_key", length = 128, nullable = false, unique = true)
    private String sourceKey;

    @Column(name = "sheet_url", length = 2048, nullable = false)
    private String sheetUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_source_type", length = 32, nullable = false)
    private GroupSourceType defaultSourceType = GroupSourceType.STANDARD;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
