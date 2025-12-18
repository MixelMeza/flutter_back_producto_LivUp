package edu.pe.residencias.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pe.residencias.model.entity.Backup;
import edu.pe.residencias.model.entity.Usuario;
import edu.pe.residencias.repository.UsuarioRepository;
import edu.pe.residencias.security.JwtUtil;
import edu.pe.residencias.service.BackupService;

@RestController
@RequestMapping("/api/admin/backups")
public class BackupController {

    @Autowired
    private BackupService backupService;

    @Autowired
    private edu.pe.residencias.service.UsuarioService usuarioService;

    private boolean isAdmin(HttpServletRequest request) {
        Object claimsObj = request.getAttribute("jwtClaims");
        if (claimsObj == null) return false;
        try {
            io.jsonwebtoken.Claims claims = (io.jsonwebtoken.Claims) claimsObj;
            String role = claims.get("role", String.class);
            return role != null && "admin".equalsIgnoreCase(role);
        } catch (Exception e) { return false; }
    }

    private Usuario getRequestingUser(HttpServletRequest request) {
        Object claimsObj = request.getAttribute("jwtClaims");
        if (claimsObj == null) return null;
        try {
            io.jsonwebtoken.Claims claims = (io.jsonwebtoken.Claims) claimsObj;
            String username = claims.get("user", String.class);
            if (username == null) return null;
            return usuarioService.findByUsernameOrEmail(username).orElse(null);
        } catch (Exception e) { return null; }
    }

    
    @PostMapping
    public ResponseEntity<?> createBackup(@RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
        if (!isAdmin(request)) return new ResponseEntity<>(Map.of("error", "Forbidden"), HttpStatus.FORBIDDEN);
        String name = null;
        if (body != null) name = body.get("name");
        Usuario u = getRequestingUser(request);
        try {
            Backup b = backupService.createBackup(name, u);
            return new ResponseEntity<>(Map.of("id", b.getId(), "name", b.getName(), "createdAt", b.getCreatedAt()), HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(Map.of("error", "Failed to create backup", "detail", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<?> listBackups(HttpServletRequest request) {
        if (!isAdmin(request)) return new ResponseEntity<>(Map.of("error", "Forbidden"), HttpStatus.FORBIDDEN);
        List<Backup> list = backupService.listBackups();
        // return metadata only
        var out = list.stream().map(b -> Map.of(
            "id", b.getId(),
            "name", b.getName(),
            "createdAt", b.getCreatedAt(),
            "sizeBytes", b.getSizeBytes()
        )).toList();
        return ResponseEntity.ok(out);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> downloadBackup(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) {
        if (!isAdmin(request)) return new ResponseEntity<>(Map.of("error", "Forbidden"), HttpStatus.FORBIDDEN);
        var opt = backupService.findById(id);
        if (opt.isEmpty()) return new ResponseEntity<>(Map.of("error", "Not found"), HttpStatus.NOT_FOUND);
        Backup b = opt.get();
        try {
            String filename = URLEncoder.encode(b.getName() + "-" + b.getId() + ".json.gz", StandardCharsets.UTF_8);
            response.setContentType(b.getMimeType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : b.getMimeType());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentLengthLong(b.getSizeBytes() == null ? b.getContent().length : b.getSizeBytes());
            response.getOutputStream().write(b.getContent());
            response.flushBuffer();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(Map.of("error", "Failed to stream backup", "detail", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBackup(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) return new ResponseEntity<>(Map.of("error", "Forbidden"), HttpStatus.FORBIDDEN);
        try {
            backupService.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("error", "Failed to delete", "detail", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editBackup(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        if (!isAdmin(request)) return new ResponseEntity<>(Map.of("error", "Forbidden"), HttpStatus.FORBIDDEN);
        String newName = body == null ? null : body.get("name");
        try {
            if (newName == null || newName.isBlank()) return new ResponseEntity<>(Map.of("error", "name required"), HttpStatus.BAD_REQUEST);
            Backup updated = backupService.updateName(id, newName);
            return ResponseEntity.ok(Map.of("id", updated.getId(), "name", updated.getName(), "createdAt", updated.getCreatedAt()));
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("error", "Failed to update", "detail", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<?> restoreBackup(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        if (!isAdmin(request)) return new ResponseEntity<>(Map.of("error", "Forbidden"), HttpStatus.FORBIDDEN);
        boolean truncate = false;
        if (body != null && body.containsKey("truncate")) {
            Object t = body.get("truncate");
            if (t instanceof Boolean) truncate = (Boolean) t;
            else if (t instanceof String) truncate = Boolean.parseBoolean((String) t);
        }
        try {
            backupService.restoreBackup(id, truncate);
            return ResponseEntity.ok(Map.of("restored", id, "truncate", truncate));
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(Map.of("error", "Failed to restore", "detail", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/cleanup")
    public ResponseEntity<?> cleanupOldBackups(HttpServletRequest request) {
        if (!isAdmin(request)) return new ResponseEntity<>(Map.of("error", "Forbidden"), HttpStatus.FORBIDDEN);
        try {
            var list = backupService.listBackups();
            var cutoff = edu.pe.residencias.util.DateTimeUtil.nowLima().minusDays(30);
            long removed = 0;
            for (var b : list) {
                if (b.getCreatedAt() != null && b.getCreatedAt().isBefore(cutoff)) {
                    backupService.deleteById(b.getId());
                    removed++;
                }
            }
            return ResponseEntity.ok(Map.of("deleted", removed));
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(Map.of("error", "Failed to cleanup", "detail", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadByDate(@RequestParam(name = "date", required = false) String dateStr,
                                            HttpServletRequest request, HttpServletResponse response) {
        if (!isAdmin(request)) return new ResponseEntity<>(Map.of("error", "Forbidden"), HttpStatus.FORBIDDEN);
        try {
            java.util.Optional<Backup> opt;
            if (dateStr == null || dateStr.isBlank()) {
                opt = backupService.findMostRecent();
            } else {
                java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
                opt = backupService.findLatestByDate(date);
            }
            if (opt.isEmpty()) return new ResponseEntity<>(Map.of("error", "Not found"), HttpStatus.NOT_FOUND);
            Backup b = opt.get();
            String filename = URLEncoder.encode(b.getName() + "-" + b.getId() + ".json.gz", StandardCharsets.UTF_8);
            response.setContentType(b.getMimeType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : b.getMimeType());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentLengthLong(b.getSizeBytes() == null ? b.getContent().length : b.getSizeBytes());
            response.getOutputStream().write(b.getContent());
            response.flushBuffer();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(Map.of("error", "Failed to download", "detail", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/upload-restore")
    public ResponseEntity<?> uploadAndRestore(@RequestParam(value = "name", required = false) String name,
                                              @RequestPart("file") MultipartFile file,
                                              @RequestParam(value = "truncate", required = false, defaultValue = "false") boolean truncate,
                                              HttpServletRequest request) {
        if (!isAdmin(request)) return new ResponseEntity<>(Map.of("error", "Forbidden"), HttpStatus.FORBIDDEN);
        try {
            if (file == null || file.isEmpty()) return new ResponseEntity<>(Map.of("error", "file required"), HttpStatus.BAD_REQUEST);
            Usuario u = getRequestingUser(request);
            byte[] content = file.getBytes();
            String mime = file.getContentType();
            Backup b = backupService.saveUploadedBackup(name, content, mime, u);
            // restore immediately
            backupService.restoreBackup(b.getId(), truncate);
            return ResponseEntity.ok(Map.of("restoredId", b.getId(), "name", b.getName(), "truncate", truncate));
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(Map.of("error", "Failed to upload and restore", "detail", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

