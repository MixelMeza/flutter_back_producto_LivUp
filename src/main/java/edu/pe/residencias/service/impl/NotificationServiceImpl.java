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
import edu.pe.residencias.service.NotificationService;
import jakarta.annotation.PostConstruct;

@Service
public class NotificationServiceImpl implements NotificationService {
    
    @Autowired
    private DeviceTokenRepository deviceTokenRepository;
    
    @Autowired
    private NotificacionRepository notificacionRepository;
    
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
        
        List<DeviceToken> tokens = deviceTokenRepository.findByUsuarioIdAndIsActiveTrue(usuario.getId());
        
        for (DeviceToken deviceToken : tokens) {
            try {
                Message message = Message.builder()
                    .setToken(deviceToken.getFcmToken())
                    .setNotification(Notification.builder()
                        .setTitle(titulo)
                        .setBody(mensaje)
                        .build())
                    .putAllData(data != null ? data : new HashMap<>())
                    .build();
                
                String response = FirebaseMessaging.getInstance().send(message);
                System.out.println("[NotificationService] Push enviado: " + response);
            } catch (Exception e) {
                System.err.println("[NotificationService] Error al enviar push a token " + deviceToken.getFcmToken() + ": " + e.getMessage());
                // Si el token es inválido, marcarlo como inactivo
                if (e.getMessage().contains("invalid") || e.getMessage().contains("not-found")) {
                    deviceToken.setIsActive(false);
                    deviceTokenRepository.save(deviceToken);
                }
            }
        }
    }
    
    @Override
    public void sendPushNotificationToMultipleUsers(List<Usuario> usuarios, String titulo, String mensaje, Map<String, String> data) {
        for (Usuario usuario : usuarios) {
            sendPushNotification(usuario, titulo, mensaje, data);
        }
    }
    
    @Override
    @Transactional
    public void registerDeviceToken(Usuario usuario, String fcmToken, String deviceType, String deviceName) {
        // Verificar si el token ya existe
        var existingToken = deviceTokenRepository.findByFcmToken(fcmToken);
        
        if (existingToken.isPresent()) {
            // Actualizar el token existente
            DeviceToken token = existingToken.get();
            token.setUsuario(usuario);
            token.setDeviceType(deviceType);
            token.setDeviceName(deviceName);
            token.setIsActive(true);
            deviceTokenRepository.save(token);
        } else {
            // Crear nuevo token
            DeviceToken newToken = new DeviceToken();
            newToken.setUsuario(usuario);
            newToken.setFcmToken(fcmToken);
            newToken.setDeviceType(deviceType);
            newToken.setDeviceName(deviceName);
            newToken.setIsActive(true);
            deviceTokenRepository.save(newToken);
        }
    }
    
    @Override
    @Transactional
    public void unregisterDeviceToken(String fcmToken) {
        deviceTokenRepository.deleteByFcmToken(fcmToken);
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
