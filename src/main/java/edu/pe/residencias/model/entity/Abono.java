package edu.pe.residencias.model.entity;

import java.math.BigDecimal;
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
import lombok.NoArgsConstructor;

@Entity
@Table(name = "abonos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Abono {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    @Column(name = "monto_abonado")
    private BigDecimal montoAbonado;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    @Column(name = "metodo_pago")
    private String metodoPago;

        @jakarta.persistence.PrePersist
        public void prePersist() {
            if (this.fecha == null) {
                this.fecha = java.time.LocalDateTime.now();
            }
        }
}
