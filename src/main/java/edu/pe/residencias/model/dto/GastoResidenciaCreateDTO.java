package edu.pe.residencias.model.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GastoResidenciaCreateDTO {
    @NotNull
    private Long residenciaId;

    private String concepto;

    private String descripcion;

    /** Tipo de gasto (AGUA, LUZ, INTERNET, GAS, MANTENIMIENTO, LIMPIEZA, OTRO) - opcional */
    private String tipoGasto;

    /** Periodo formato "YYYY-MM" o similar - opcional */
    private String periodo;

    /** Fecha del gasto en ISO date: yyyy-MM-dd - opcional */
    private String fechaGasto;

    private BigDecimal monto;

    /** Estado de pago (PENDIENTE, PAGADO, VENCIDO) - opcional, se ignora y se pone PENDIENTE por defecto */
    private String estadoPago;

    /** Metodo de pago requerido: EFECTIVO, TRANSFERENCIA, YAPE, PLIN, TARJETA */
    @NotNull
    private String metodoPago;

    private String comprobanteUrl;
}
