package edu.pe.residencias.model.entity;

import java.math.BigDecimal;
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
@Table(name = "habitaciones")
public class Habitacion {
        @jakarta.persistence.PrePersist
        public void prePersist() {
            if (this.createdAt == null) {
                this.createdAt = java.time.LocalDateTime.now();
            }
        }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "residencia_id", nullable = false)
    private Residencia residencia;

    @Column(name = "codigo_habitacion")
    private String codigoHabitacion;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "departamento")
    private Boolean departamento;

    @Column(name = "bano_privado")
    private Boolean banoPrivado;

    @Column(name = "wifi")
    private Boolean wifi;

    @Column(name = "amueblado")
    private Boolean amueblado;

    @Column(name = "piso")
    private Integer piso;

    @Column(name = "capacidad")
    private Integer capacidad;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "permitir_mascotas")
    private Boolean permitir_mascotas;

    @Column(name = "agua")
    private Boolean agua;

    @Column(name = "luz")
    private Boolean luz;

    @Column(name = "precio_mensual", precision = 10, scale = 2)
    private BigDecimal precioMensual;

    @Column(name = "estado")
    private String estado;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "habitacion")
    @JsonIgnore
    private Set<SolicitudAlojamiento> solicitudesAlojamiento;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "habitacion")
    @JsonIgnore
    private Set<ImagenHabitacion> imagenesHabitacion;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Habitacion that = (Habitacion) o;
            return id != null && id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }
}
