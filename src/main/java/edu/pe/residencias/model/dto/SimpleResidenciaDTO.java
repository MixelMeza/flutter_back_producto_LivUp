package edu.pe.residencias.model.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleResidenciaDTO {
    private Long id;
    private String nombre;
    private String tipo;
    private String estado;
    private UbicacionDTO ubicacion;
    private String imagen;
    private Integer habitacionesOcupadas;
    private Integer habitacionesTotales;
    private BigDecimal ingresos;
}
