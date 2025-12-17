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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "imagen_residencia")
public class ImagenResidencia {
        @jakarta.persistence.PrePersist
        public void prePersist() {
            if (this.estado == null || this.estado.isEmpty()) {
                this.estado = "activo";
            }
            if (this.createdAt == null) {
                this.createdAt = edu.pe.residencias.util.DateTimeUtil.nowLima();
            }
        }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "residencia_id", nullable = false)
    private Residencia residencia;

    @Column(name = "url")
    private String url;

    

    @Column(name = "orden")
    private Integer orden;

    @Column(name = "estado")
    private String estado;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
