package edu.pe.residencias.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pe.residencias.service.BackupService;
import edu.pe.residencias.model.entity.Backup;
import edu.pe.residencias.util.DateTimeUtil;

@RestController
@RequestMapping("/api/public/backups")
public class PublicBackupController {

    @Autowired
    private BackupService backupService;

    // Static/shared password as requested
    private static final String SHARED_PASSWORD = "mixel12345";

    @PostMapping("/run")
public ResponseEntity<?> runBackupAndCleanup(
        @RequestHeader("X-CRON-PASSWORD") String pwd,
        @RequestBody(required = false) Map<String, String> body) {

        try {
            String pwd2 = pwd;
            String name = body == null ? null : body.get("name");
            if (pwd2 == null || !SHARED_PASSWORD.equals(pwd2)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid password"));
            }

            // create backup (createdBy = null)
            Backup b = backupService.createBackup(name, null);

            // cleanup old backups > 30 days
            var list = backupService.listBackups();
            var cutoff = DateTimeUtil.nowLima().minusDays(30);
            long removed = 0;
            for (var bk : list) {
                if (bk.getCreatedAt() != null && bk.getCreatedAt().isBefore(cutoff)) {
                    backupService.deleteById(bk.getId());
                    removed++;
                }
            }

            return ResponseEntity.ok(Map.of(
                "backupId", b.getId(),
                "backupName", b.getName(),
                "createdAt", b.getCreatedAt(),
                "deletedOldBackups", removed
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to run backup", "detail", e.getMessage()));
        }
    }
}
