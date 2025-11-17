package edu.pe.residencias.model.entity;

import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
@Table(name = "residencias")
public class Residencia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "universidad_id", nullable = false)
    private Universidad universidad;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "tipo")
    private String tipo;

    @Column(name = "cantidad_habitaciones")
    private Integer cantidadHabitaciones;

    @Column(name = "reglamento_url")
    private String reglamentoUrl;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario; // usuario administrador

    @ManyToOne
    @JoinColumn(name = "ubicacion_id", nullable = false)
    private Ubicacion ubicacion;

    @Column(name = "telefono_contacto")
    private String telefonoContacto;

    @Column(name = "email_contacto")
    private String emailContacto;

    @Column(name = "servicios", columnDefinition = "TEXT")
    private String servicios;

    @Column(name = "estado")
    private String estado = "Activo";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "residencia")
    @JsonIgnore
    private Set<Habitacion> habitaciones;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "residencia")
    @JsonIgnore
    private Set<SolicitudAlojamiento> solicitudesAlojamiento;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "residencia")
    @JsonIgnore
    private Set<GastoResidencia> gastosResidencia;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "residencia")
    @JsonIgnore
    private Set<Review> reviews;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "residencia")
    @JsonIgnore
    private Set<ImagenResidencia> imagenesResidencia;
}
