package edu.pe.residencias.model.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class HabitacionStatsCardDTO {
    private Long habitacionId;
    private String nombreHabitacion;
    private Boolean departamento;
    private BigDecimal precioMensual;

    private String nombreResidencia;

    private BigDecimal lat;
    private BigDecimal lng;

    private Long imagenPrincipalId;

    private Boolean liked;
}
