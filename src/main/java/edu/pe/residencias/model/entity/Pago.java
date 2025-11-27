package edu.pe.residencias.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import edu.pe.residencias.model.enums.PagoEstado;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Data
@Table(name = "pagos")
public class Pago {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contrato contrato;

    @Column(name = "monto", precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "metodo_pago")
    private String metodoPago; // 'tarjeta', 'transferencia', 'efectivo', 'paypal'

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "estado")
    @Enumerated(EnumType.STRING)
    private PagoEstado estado; // pendiente, completado, fallido

    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Abono> abonos;

        @jakarta.persistence.PrePersist
        public void prePersist() {
            if (this.fechaPago == null) {
                this.fechaPago = java.time.LocalDateTime.now();
            }
        }
}
