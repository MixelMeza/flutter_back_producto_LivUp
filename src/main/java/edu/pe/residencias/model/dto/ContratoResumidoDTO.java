package edu.pe.residencias.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoResumidoDTO {
    private Long id;
    private String estudiante; // Nombre del estudiante
    private String email; // Email del estudiante
    private String habitacion; // Código/nombre de la habitación
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private LocalDate fechaProximaRenovacion;
    private BigDecimal montoTotal;
    private String estado; // vigente, finalizado, cancelado, renovacion
}
