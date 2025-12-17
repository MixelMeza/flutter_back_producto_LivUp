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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Column(name = "tipo", nullable = false)
    private String tipo; // SOLICITUD_NUEVA, SOLICITUD_ACEPTADA, SOLICITUD_RECHAZADA, CONTRATO_NUEVO, PAGO_RECIBIDO, etc.
    
    @Column(name = "titulo", nullable = false)
    private String titulo;
    
    @Column(name = "mensaje", columnDefinition = "TEXT")
    private String mensaje;
    
    @Column(name = "entidad_tipo")
    private String entidadTipo; // Solicitud, Contrato, Pago, etc.
    
    @Column(name = "entidad_id")
    private Long entidadId;
    
    @Column(name = "leida")
    private Boolean leida = false;
    
    @Column(name = "enviada_push")
    private Boolean enviadaPush = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = edu.pe.residencias.util.DateTimeUtil.nowLima();
    }
}
