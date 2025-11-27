package edu.pe.residencias.service;

import java.util.List;
import java.util.Map;

import edu.pe.residencias.model.entity.Usuario;

public interface NotificationService {
    
    /**
     * Envía una notificación push a un usuario específico
     */
    void sendPushNotification(Usuario usuario, String titulo, String mensaje, Map<String, String> data);
    
    /**
     * Envía una notificación push a múltiples usuarios
     */
    void sendPushNotificationToMultipleUsers(List<Usuario> usuarios, String titulo, String mensaje, Map<String, String> data);
    
    /**
     * Registra un token FCM para un usuario
     */
    void registerDeviceToken(Usuario usuario, String fcmToken, String deviceType, String deviceName);
    
    /**
     * Elimina un token FCM
     */
    void unregisterDeviceToken(String fcmToken);
    
    /**
     * Crea una notificación en la base de datos
     */
    void createNotification(Usuario usuario, String tipo, String titulo, String mensaje, String entidadTipo, Long entidadId);
    
    /**
     * Obtiene todas las notificaciones de un usuario
     */
    List<edu.pe.residencias.model.entity.Notificacion> getUserNotifications(Long usuarioId);
    
    /**
     * Obtiene notificaciones no leídas de un usuario
     */
    List<edu.pe.residencias.model.entity.Notificacion> getUnreadNotifications(Long usuarioId);
    
    /**
     * Marca una notificación como leída
     */
    void markAsRead(Long notificacionId);
    
    /**
     * Marca todas las notificaciones de un usuario como leídas
     */
    void markAllAsRead(Long usuarioId);
    
    /**
     * Cuenta notificaciones no leídas
     */
    long countUnreadNotifications(Long usuarioId);
}
