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

    @org.springframework.beans.factory.annotation.Value("${backups.auto.enabled:false}")
    private boolean backupsAutoEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (!backupsAutoEnabled) {
            logger.info("Startup backup disabled by configuration (backups.auto.enabled=false)");
            return;
        }
        try {
            logger.info("Triggering startup DB backup");
            backupService.backupNow("startup");
        } catch (Exception e) {
            logger.warn("Startup backup failed", e);
        }
    }

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        if (!backupsAutoEnabled) {
            logger.info("Shutdown backup disabled by configuration (backups.auto.enabled=false)");
            return;
        }
        try {
            logger.info("Triggering shutdown DB backup");
            backupService.backupNow("shutdown");
        } catch (Exception e) {
            logger.warn("Shutdown backup failed", e);
        }
    }
}
