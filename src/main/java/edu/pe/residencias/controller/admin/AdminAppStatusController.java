package edu.pe.residencias.controller.admin;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pe.residencias.service.AppStatusService;

@RestController
@RequestMapping("/api/admin/app-status")
public class AdminAppStatusController {

    @Autowired
    private AppStatusService appStatusService;

    private boolean isAdmin(HttpServletRequest request) {
        Object claimsObj = request.getAttribute("jwtClaims");
        if (claimsObj == null) return false;
        try {
            io.jsonwebtoken.Claims claims = (io.jsonwebtoken.Claims) claimsObj;
            String role = claims.get("role", String.class);
            return role != null && "admin".equalsIgnoreCase(role);
        } catch (Exception e) {
            return false;
        }
    }

    @PostMapping("")
    public ResponseEntity<?> setStatus(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        if (!isAdmin(request)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo administradores"));
        boolean maintenance = false;
        String message = null;
        if (body != null) {
            Object m = body.get("maintenance");
            if (m instanceof Boolean) maintenance = (Boolean) m;
            else if (m != null) maintenance = Boolean.parseBoolean(m.toString());
            Object msg = body.get("message");
            if (msg != null) message = msg.toString();
        }
        appStatusService.setMaintenance(maintenance, message);
        return ResponseEntity.ok(appStatusService.getStatus());
    }

    @DeleteMapping("")
    public ResponseEntity<?> clearStatus(HttpServletRequest request) {
        if (!isAdmin(request)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo administradores"));
        appStatusService.clear();
        return ResponseEntity.ok(appStatusService.getStatus());
    }
}
