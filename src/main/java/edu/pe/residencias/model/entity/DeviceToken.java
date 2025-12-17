package edu.pe.residencias.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "device_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Column(name = "fcm_token", nullable = false, unique = true, length = 500)
    private String fcmToken;
    
    @Column(name = "device_type")
    private String deviceType; // android, ios, web
    
    @Column(name = "device_name")
    private String deviceName;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = edu.pe.residencias.util.DateTimeUtil.nowLima();
        updatedAt = edu.pe.residencias.util.DateTimeUtil.nowLima();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = edu.pe.residencias.util.DateTimeUtil.nowLima();
    }
}
