package edu.pe.residencias.model.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UbicacionDTO {
    private Long id;
    private String direccion;
    private String departamento;
    private String distrito;
    private String provincia;
    private String pais;
    private BigDecimal latitud;
    private BigDecimal longitud;
}
