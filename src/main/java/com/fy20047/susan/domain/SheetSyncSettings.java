package com.fy20047.susan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sheet_sync_settings")
@Getter
@Setter
@NoArgsConstructor
public class SheetSyncSettings {

    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "auto_sync_enabled", nullable = false)
    private Boolean autoSyncEnabled = false;

    @Column(name = "default_sources_initialized", nullable = false)
    private Boolean defaultSourcesInitialized = false;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
