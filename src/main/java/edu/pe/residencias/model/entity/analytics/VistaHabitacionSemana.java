package edu.pe.residencias.model.entity.analytics;

import edu.pe.residencias.model.entity.Habitacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "vistas_habitacion_semana",
        uniqueConstraints = @UniqueConstraint(columnNames = {"habitacion_id", "week_key"})
)
public class VistaHabitacionSemana {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habitacion_id", nullable = false)
    private Habitacion habitacion;

    @Column(name = "week_key", nullable = false, length = 8)
    private String weekKey;

    @Column(name = "total_vistas", nullable = false)
    private Long totalVistas = 0L;
}
