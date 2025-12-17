package edu.pe.residencias.service.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import edu.pe.residencias.model.entity.DeviceToken;
import edu.pe.residencias.model.entity.Usuario;
import edu.pe.residencias.repository.DeviceTokenRepository;
import edu.pe.residencias.repository.NotificacionRepository;
import edu.pe.residencias.repository.AccesoRepository;
import edu.pe.residencias.service.NotificationService;
import jakarta.annotation.PostConstruct;
import edu.pe.residencias.model.entity.Dispositivo;
import edu.pe.residencias.model.enums.UsuarioEstado;

@Service
public class NotificationServiceImpl implements NotificationService {
    
    @Autowired
    private DeviceTokenRepository deviceTokenRepository; // legacy - will prefer Dispositivo

    @Autowired
    private edu.pe.residencias.repository.DispositivoRepository dispositivoRepository;

    @Autowired
    private edu.pe.residencias.service.DispositivoService dispositivoService;
    
    @Autowired
    private NotificacionRepository notificacionRepository;
    
    @Autowired
    private AccesoRepository accesoRepository;
    
    @Value("${firebase.config.path:#{null}}")
    private String firebaseConfigPath;
    
    private boolean firebaseInitialized = false;
    
    @PostConstruct
    public void initialize() {
        try {
            if (firebaseConfigPath != null && !firebaseConfigPath.isEmpty()) {
                FileInputStream serviceAccount = new FileInputStream(firebaseConfigPath);
                
                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
                
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                }
                
                firebaseInitialized = true;
                System.out.println("[NotificationService] Firebase inicializado correctamente");
            } else {
                System.out.println("[NotificationService] Firebase config no configurado. Notificaciones push deshabilitadas.");
            }
        } catch (IOException e) {
            System.err.println("[NotificationService] Error al inicializar Firebase: " + e.getMessage());
            firebaseInitialized = false;
        }
    }
    
    @Override
    public void sendPushNotification(Usuario usuario, String titulo, String mensaje, Map<String, String> data) {
        if (!firebaseInitialized) {
            System.out.println("[NotificationService] Firebase no inicializado. Omitiendo push notification.");
            return;
        }
        // Do not send to suspended users
        if (usuario == null || usuario.getEstado() == null || usuario.getEstado() != UsuarioEstado.ACTIVO) {
            System.out.println("[NotificationService] Usuario no activo o nulo, omitiendo push: " + (usuario == null ? "null" : usuario.getId()));
            return;
        }
        // Prefer using `dispositivos` table for active device tokens
        List<Dispositivo> devices = dispositivoRepository.findByUsuarioIdAndActivoTrue(usuario.getId());
        // If none, fallback to legacy device tokens
        if (devices == null || devices.isEmpty()) {
            List<DeviceToken> tokens = deviceTokenRepository.findByUsuarioIdAndIsActiveTrue(usuario.getId());
            if (tokens == null || tokens.isEmpty()) return;
            for (DeviceToken deviceToken : tokens) {
                sendToTokenWithRetries(deviceToken.getFcmToken(), titulo, mensaje, data, null);
            }
            return;
        }

        for (Dispositivo device : devices) {
            if (device == null) continue;
            if (Boolean.FALSE.equals(device.getActivo()) || Boolean.TRUE.equals(device.getBloqueado())) continue;
            sendToTokenWithRetries(device.getFcmToken(), titulo, mensaje, data, device);
        }
    }
    
    @Override
    public void sendPushNotificationToMultipleUsers(List<Usuario> usuarios, String titulo, String mensaje, Map<String, String> data) {
        if (!firebaseInitialized) {
            System.out.println("[NotificationService] Firebase no inicializado. Omitiendo push notification (multi).");
            return;
        }
        // Build unique set of tokens for active devices of given users (do not use accesos)
        java.util.Set<String> tokensToSend = new java.util.HashSet<>();
        java.util.Map<String, Dispositivo> tokenToDevice = new java.util.HashMap<>();

        for (Usuario usuario : usuarios) {
            if (usuario == null || usuario.getEstado() == null || usuario.getEstado() != UsuarioEstado.ACTIVO) {
                System.out.println("[NotificationService] Skipping inactive user for multi-send: " + (usuario == null ? "null" : usuario.getId()));
                continue;
            }
            try {
                List<Dispositivo> devices = dispositivoRepository.findByUsuarioIdAndActivoTrue(usuario.getId());
                if (devices != null) {
                    for (Dispositivo d : devices) {
                        if (d == null) continue;
                        if (Boolean.FALSE.equals(d.getActivo()) || Boolean.TRUE.equals(d.getBloqueado())) continue;
                        if (d.getFcmToken() == null || d.getFcmToken().isBlank()) continue;
                        tokensToSend.add(d.getFcmToken());
                        tokenToDevice.put(d.getFcmToken(), d);
                    }
                }
                // fallback to legacy tokens for this user if needed
                List<DeviceToken> dtokens = deviceTokenRepository.findByUsuarioIdAndIsActiveTrue(usuario.getId());
                if (dtokens != null) {
                    for (DeviceToken dt : dtokens) {
                        if (dt.getFcmToken() != null && !dt.getFcmToken().isBlank()) tokensToSend.add(dt.getFcmToken());
                    }
                }
            } catch (Exception e) {
                System.err.println("[NotificationService] Error collecting devices for user " + usuario.getId() + ": " + e.getMessage());
            }
        }

        // Send one message per unique token
        for (String token : tokensToSend) {
            Dispositivo d = tokenToDevice.get(token);
            sendToTokenWithRetries(token, titulo, mensaje, data, d);
        }
    }

    // Helper: send with retry and mark device inactive on permanent errors
    private void sendToTokenWithRetries(String token, String titulo, String mensaje, Map<String, String> data, Dispositivo device) {
        if (token == null || token.isBlank()) return;
        int attempts = 0;
        int maxAttempts = 3;
        long backoff = 500; // ms
        while (attempts < maxAttempts) {
            attempts++;
            try {
                Message.Builder mb = Message.builder()
                        .setToken(token)
                        .setNotification(Notification.builder().setTitle(titulo).setBody(mensaje).build());
                if (data != null && !data.isEmpty()) mb.putAllData(data);
                Message message = mb.build();
                String response = FirebaseMessaging.getInstance().send(message);
                System.out.println("[NotificationService] Push enviado a token=" + token + " response=" + response);
                return;
            } catch (com.google.firebase.messaging.FirebaseMessagingException fme) {
                String code = fme.getErrorCode() == null ? "" : fme.getErrorCode().name();
                String msg = fme.getMessage() == null ? "" : fme.getMessage().toLowerCase();
                // Permanent errors: invalid argument / registration token not registered
                if (msg.contains("notregistered") || msg.contains("not registered") || msg.contains("registration-token-not-registered") || msg.contains("invalid-argument") || code.equalsIgnoreCase("INVALID_ARGUMENT") || code.equalsIgnoreCase("NOT_FOUND")) {
                    // mark device inactive if available
                    if (device != null) {
                        try {
                            device.setActivo(false);
                            dispositivoRepository.save(device);
                            System.out.println("[NotificationService] Marcado dispositivo.activo=false por token inválido: " + token);
                        } catch (Exception ex) { System.err.println("[NotificationService] Error marcando dispositivo inactivo: " + ex.getMessage()); }
                    } else {
                        // fallback: update legacy token state
                        try { deviceTokenRepository.findByFcmToken(token).ifPresent(dt -> { dt.setIsActive(false); deviceTokenRepository.save(dt); }); } catch (Exception inner) {}
                    }
                    return;
                }
                // Transient errors -> retry
                if (msg.contains("unavailable") || msg.contains("internal") || code.equalsIgnoreCase("UNAVAILABLE") || code.equalsIgnoreCase("INTERNAL")) {
                    try { Thread.sleep(backoff); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    backoff *= 2;
                    continue;
                }
                // default: do not retry
                System.err.println("[NotificationService] FirebaseMessagingException non-retry for token=" + token + ", msg=" + fme.getMessage());
                return;
            } catch (Exception e) {
                // treat as transient network error -> retry a few times
                System.err.println("[NotificationService] Error sending push to token=" + token + ", attempt=" + attempts + ", err=" + e.getMessage());
                try { Thread.sleep(backoff); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                backoff *= 2;
            }
        }
        System.err.println("[NotificationService] Exhausted retries for token=" + token);
    }
    
    @Override
    @Transactional
    public void registerDeviceToken(Usuario usuario, String fcmToken, String deviceType, String deviceName) {
        // Prefer creating/updating in dispositivos table
        try {
            // Use DispositivoService to centralize creation/update/reassignment
            Long usuarioId = usuario != null ? usuario.getId() : null;
            dispositivoService.registerOrUpdate(fcmToken, deviceType, deviceName, null, usuarioId);
        } catch (Exception e) {
            // fallback to legacy device token table
            var existingToken = deviceTokenRepository.findByFcmToken(fcmToken);
            if (existingToken.isPresent()) {
                DeviceToken token = existingToken.get();
                token.setUsuario(usuario);
                token.setDeviceType(deviceType);
                token.setDeviceName(deviceName);
                token.setIsActive(true);
                deviceTokenRepository.save(token);
            } else {
                DeviceToken newToken = new DeviceToken();
                newToken.setUsuario(usuario);
                newToken.setFcmToken(fcmToken);
                newToken.setDeviceType(deviceType);
                newToken.setDeviceName(deviceName);
                newToken.setIsActive(true);
                deviceTokenRepository.save(newToken);
            }
        }
    }
    
    @Override
    @Transactional
    public void unregisterDeviceToken(String fcmToken) {
        try {
            dispositivoService.findByFcmToken(fcmToken).ifPresent(d -> dispositivoService.deleteById(d.getId()));
        } catch (Exception e) {
            deviceTokenRepository.deleteByFcmToken(fcmToken);
        }
    }
    
    @Override
    @Transactional
    public void createNotification(Usuario usuario, String tipo, String titulo, String mensaje, String entidadTipo, Long entidadId) {
        // Crear notificación en la base de datos
        edu.pe.residencias.model.entity.Notificacion notificacion = new edu.pe.residencias.model.entity.Notificacion();
        notificacion.setUsuario(usuario);
        notificacion.setTipo(tipo);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setEntidadTipo(entidadTipo);
        notificacion.setEntidadId(entidadId);
        notificacion.setLeida(false);
        
        notificacionRepository.save(notificacion);
        
        // Enviar push notification
        Map<String, String> data = new HashMap<>();
        data.put("tipo", tipo);
        data.put("entidadTipo", entidadTipo != null ? entidadTipo : "");
        data.put("entidadId", entidadId != null ? entidadId.toString() : "");
        
        sendPushNotification(usuario, titulo, mensaje, data);
        
        notificacion.setEnviadaPush(true);
        notificacionRepository.save(notificacion);
    }
    
    @Override
    public List<edu.pe.residencias.model.entity.Notificacion> getUserNotifications(Long usuarioId) {
        return notificacionRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId);
    }
    
    @Override
    public List<edu.pe.residencias.model.entity.Notificacion> getUnreadNotifications(Long usuarioId) {
        return notificacionRepository.findByUsuarioIdAndLeidaFalseOrderByCreatedAtDesc(usuarioId);
    }
    
    @Override
    @Transactional
    public void markAsRead(Long notificacionId) {
        notificacionRepository.findById(notificacionId).ifPresent(notif -> {
            notif.setLeida(true);
            notificacionRepository.save(notif);
        });
    }
    
    @Override
    @Transactional
    public void markAllAsRead(Long usuarioId) {
        List<edu.pe.residencias.model.entity.Notificacion> notificaciones = 
            notificacionRepository.findByUsuarioIdAndLeidaFalseOrderByCreatedAtDesc(usuarioId);
        
        notificaciones.forEach(notif -> notif.setLeida(true));
        notificacionRepository.saveAll(notificaciones);
    }
    
    @Override
    public long countUnreadNotifications(Long usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }
}
