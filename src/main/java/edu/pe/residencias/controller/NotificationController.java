package edu.pe.residencias.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pe.residencias.model.entity.Notificacion;
import edu.pe.residencias.model.entity.Usuario;
import edu.pe.residencias.repository.UsuarioRepository;
import edu.pe.residencias.security.JwtUtil;
import edu.pe.residencias.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    /**
     * Registrar token FCM del dispositivo
     */
    @PostMapping("/register-token")
    public ResponseEntity<?> registerToken(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        try {
            String token = authHeader.substring(7);
            UUID uid = jwtUtil.extractUUID(token);
            Usuario usuario = usuarioRepository.findByUuid(uid.toString())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            String fcmToken = body.get("fcmToken");
            String deviceType = body.get("deviceType");
            String deviceName = body.get("deviceName");
            
            if (fcmToken == null || fcmToken.isBlank()) {
                return ResponseEntity.badRequest().body("FCM token requerido");
            }
            
            notificationService.registerDeviceToken(usuario, fcmToken, deviceType, deviceName);
            return ResponseEntity.ok(Map.of("message", "Token registrado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al registrar token: " + e.getMessage());
        }
    }
    
    /**
     * Eliminar token FCM
     */
    @DeleteMapping("/unregister-token")
    public ResponseEntity<?> unregisterToken(@RequestBody Map<String, String> body) {
        try {
            String fcmToken = body.get("fcmToken");
            if (fcmToken == null || fcmToken.isBlank()) {
                return ResponseEntity.badRequest().body("FCM token requerido");
            }
            
            notificationService.unregisterDeviceToken(fcmToken);
            return ResponseEntity.ok(Map.of("message", "Token eliminado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al eliminar token: " + e.getMessage());
        }
    }
    
    /**
     * Obtener todas las notificaciones del usuario autenticado
     */
    @GetMapping("/my-notifications")
    public ResponseEntity<?> getMyNotifications(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            UUID uid = jwtUtil.extractUUID(token);
            Usuario usuario = usuarioRepository.findByUuid(uid.toString())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            List<Notificacion> notificaciones = notificationService.getUserNotifications(usuario.getId());
            return ResponseEntity.ok(notificaciones);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al obtener notificaciones: " + e.getMessage());
        }
    }
    
    /**
     * Obtener notificaciones no leídas
     */
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            UUID uid = jwtUtil.extractUUID(token);
            Usuario usuario = usuarioRepository.findByUuid(uid.toString())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            List<Notificacion> notificaciones = notificationService.getUnreadNotifications(usuario.getId());
            return ResponseEntity.ok(notificaciones);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al obtener notificaciones: " + e.getMessage());
        }
    }
    
    /**
     * Contar notificaciones no leídas
     */
    @GetMapping("/unread/count")
    public ResponseEntity<?> countUnread(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            UUID uid = jwtUtil.extractUUID(token);
            Usuario usuario = usuarioRepository.findByUuid(uid.toString())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            long count = notificationService.countUnreadNotifications(usuario.getId());
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al contar notificaciones: " + e.getMessage());
        }
    }
    
    /**
     * Marcar una notificación como leída
     */
    @PutMapping("/{id}/mark-as-read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            notificationService.markAsRead(id);
            return ResponseEntity.ok(Map.of("message", "Notificación marcada como leída"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al marcar notificación: " + e.getMessage());
        }
    }
    
    /**
     * Marcar todas las notificaciones como leídas
     */
    @PutMapping("/mark-all-as-read")
    public ResponseEntity<?> markAllAsRead(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            UUID uid = jwtUtil.extractUUID(token);
            Usuario usuario = usuarioRepository.findByUuid(uid.toString())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            notificationService.markAllAsRead(usuario.getId());
            return ResponseEntity.ok(Map.of("message", "Todas las notificaciones marcadas como leídas"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al marcar notificaciones: " + e.getMessage());
        }
    }
}
