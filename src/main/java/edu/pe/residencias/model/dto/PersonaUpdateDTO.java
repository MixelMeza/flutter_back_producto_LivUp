package edu.pe.residencias.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonaUpdateDTO {
    private String nombre;
    private String apellido;
    private String telefono;
    private String direccion;
    private String foto_url;
    private String fecha_nacimiento; // yyyy-MM-dd or ISO datetime
    private String email;
}
