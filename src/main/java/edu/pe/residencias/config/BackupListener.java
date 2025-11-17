package edu.pe.residencias.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BackupListener {
    private static final Logger logger = LoggerFactory.getLogger(BackupListener.class);

    @Autowired
    private DatabaseBackupService backupService;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try {
            logger.info("Triggering startup DB backup");
            backupService.backupNow("startup");
        } catch (Exception e) {
            logger.warn("Startup backup failed", e);
        }
    }

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        try {
            logger.info("Triggering shutdown DB backup");
            backupService.backupNow("shutdown");
        } catch (Exception e) {
            logger.warn("Shutdown backup failed", e);
        }
    }
}
