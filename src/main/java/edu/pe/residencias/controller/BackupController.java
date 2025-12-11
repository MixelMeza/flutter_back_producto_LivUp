package edu.pe.residencias.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.ByteArrayOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pe.residencias.service.BackupManagementService;

@RestController
@RequestMapping("/api/backups")
public class BackupController {

    @Autowired
    private BackupManagementService backupService;

    @GetMapping
    public ResponseEntity<?> list() {
        try {
            List<Map<String,Object>> list = backupService.listBackups();
            return ResponseEntity.ok(list);
        } catch (IOException ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<?> delete(@PathVariable String name) {
        try {
            boolean ok = backupService.deleteBackup(name);
            if (!ok) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Backup not found"));
            return ResponseEntity.noContent().build();
        } catch (IOException ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{name}/restore")
    public ResponseEntity<?> restore(@PathVariable String name, @RequestBody(required = false) Map<String,Object> body) {
        boolean truncate = false;
        if (body != null && body.containsKey("truncate")) truncate = Boolean.TRUE.equals(body.get("truncate"));
        try {
            backupService.restoreBackup(name, truncate);
            Map<String,Object> resp = new HashMap<>();
            resp.put("status", "restored");
            resp.put("backup", name);
            resp.put("truncate", truncate);
            return ResponseEntity.ok(resp);
        } catch (IOException ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{name}/download")
    public ResponseEntity<byte[]> download(@PathVariable String name) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            backupService.downloadBackup(name, baos);
            byte[] bytes = baos.toByteArray();
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=backup-" + name + ".zip")
                    .contentLength(bytes.length)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);
        } catch (IOException ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
