package edu.pe.residencias.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResidenciaAdminDTO {
    private Long id;
    private String nombre;
    private String tipo;
    private String ubicacion; // direccion completa
    private String propietario; // nombre del propietario
    private Integer cantidadHabitaciones;
    private Integer habitacionesOcupadas;
    private String estado; // Activo, Inactivo, Mantenimiento
    private String emailContacto;
    private String telefonoContacto;
}
