package edu.pe.residencias.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonaUpdateDTO {
    @NotBlank(message = "nombre es requerido")
    private String nombre;

    @NotBlank(message = "apellido es requerido")
    private String apellido;

    private String telefono;
    private String direccion;
    private String foto_url;
    private String fecha_nacimiento; // yyyy-MM-dd or ISO datetime
    private String email;
    private String notas;
}
