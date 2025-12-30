package edu.pe.residencias.model.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResidenciaOwnerDTO {
    private Long id;
    private String nombre;
    private String tipo;
    private Boolean destacado;
    private Integer cantidadHabitaciones;
    private String descripcion;
    private String telefonoContacto;
    private String emailContacto;
    private String servicios;
    private String estado;
    private String reglamentoUrl;
    private LocalDateTime createdAt;
    private UbicacionDTO ubicacion;
    private List<String> imagenes;
    // propietario (nombres y apellidos)
    private String propietarioNombre;
    private String propietarioApellido;
    // New minimal stats
    private Integer habitacionesOcupadas;
    private Integer habitacionesTotales;
    private java.math.BigDecimal ingresos;
}
