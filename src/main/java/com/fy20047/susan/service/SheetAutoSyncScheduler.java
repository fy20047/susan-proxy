package com.fy20047.susan.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SheetAutoSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SheetAutoSyncScheduler.class);

    private final SheetSyncService sheetSyncService;

    public SheetAutoSyncScheduler(SheetSyncService sheetSyncService) {
        this.sheetSyncService = sheetSyncService;
    }

    @Scheduled(fixedRate = 600000, initialDelay = 600000)
    public void runAutomaticSync() {
        if (!sheetSyncService.isAutoSyncEnabled()) {
            return;
        }

        try {
            sheetSyncService.syncFromGoogleSheets();
        } catch (Exception e) {
            log.error("Automatic Google Sheet sync failed.", e);
        }
    }
}
