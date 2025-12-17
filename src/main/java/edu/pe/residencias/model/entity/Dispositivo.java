package edu.pe.residencias.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "dispositivos")
public class Dispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fcm_token", nullable = false, unique = true)
    private String fcmToken;

    @Column(name = "plataforma")
    private String plataforma; // ANDROID / IOS

    @Column(name = "modelo")
    private String modelo;

    @Column(name = "os_version")
    private String osVersion;

    @Column(name = "bloqueado")
    private Boolean bloqueado = false;

    @Column(name = "activo")
    private Boolean activo = true;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario; // optional owner

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = edu.pe.residencias.util.DateTimeUtil.nowLima();
        updatedAt = edu.pe.residencias.util.DateTimeUtil.nowLima();
        if (activo == null) activo = true;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = edu.pe.residencias.util.DateTimeUtil.nowLima();
    }
}
