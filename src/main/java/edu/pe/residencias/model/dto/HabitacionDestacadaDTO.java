package edu.pe.residencias.model.dto;

import lombok.Data;

@Data
public class HabitacionDestacadaDTO {
    private Long id;
    private String nombre;
    private java.math.BigDecimal precioMensual;
    private String residenciaNombre;
    private String fotoUrl;
    private Boolean favorito;
    private Integer capacidad;
    private Integer piso;
}
