package edu.pe.residencias.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Data
@Table(name = "accesos")
public class Acceso {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "ultima_sesion")
    private LocalDateTime ultimaSesion;

    @Column(name = "ip_acceso")
    private String ipAcceso;
    /**
     * Tipo de acceso: "LOGIN" o "LOGOUT". Usado para determinar si el dispositivo
     * está actualmente con sesión iniciada por el usuario del registro de acceso más reciente.
     */
    @Column(name = "tipo")
    private String tipo;
    // New relation to dispositivos table (nullable)
    @ManyToOne
    @JoinColumn(name = "dispositivo_id")
    private Dispositivo dispositivoRel;
}
