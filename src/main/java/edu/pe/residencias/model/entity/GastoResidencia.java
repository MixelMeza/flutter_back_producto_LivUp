package edu.pe.residencias.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
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
@Table(name = "gastos_residencia")
public class GastoResidencia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "residencia_id", nullable = false)
    private Residencia residencia;

    @Column(name = "concepto", length = 100)
    private String concepto;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_gasto", length = 32)
    private TipoGasto tipoGasto;

    @Column(name = "periodo", length = 7)
    private String periodo;

    @Column(name = "fecha_gasto")
    private LocalDate fechaGasto;

    @Column(name = "monto", precision = 10, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", length = 16)
    private EstadoPago estadoPago = EstadoPago.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 24)
    private MetodoPago metodoPago;

    @Column(name = "comprobante_url", length = 255)
    private String comprobanteUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (fechaGasto == null) {
            fechaGasto = edu.pe.residencias.util.DateTimeUtil.nowLima().toLocalDate();
        }
        if (createdAt == null) createdAt = edu.pe.residencias.util.DateTimeUtil.nowLima();
    }

    @PreUpdate
    public void preUpdate() {
        if (createdAt == null) createdAt = edu.pe.residencias.util.DateTimeUtil.nowLima();
    }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GastoResidencia that = (GastoResidencia) o;
            return id != null && id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }

        public enum TipoGasto {
            AGUA, LUZ, INTERNET, GAS, MANTENIMIENTO, LIMPIEZA, OTRO
        }

        public enum EstadoPago {
            PENDIENTE, PAGADO, VENCIDO
        }

        public enum MetodoPago {
            EFECTIVO, TRANSFERENCIA, YAPE, PLIN, TARJETA
        }
}
