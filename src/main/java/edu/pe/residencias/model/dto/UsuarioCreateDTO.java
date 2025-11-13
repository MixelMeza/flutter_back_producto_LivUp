package edu.pe.residencias.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioCreateDTO {
    // Persona fields
    private String nombre;
    private String apellido;
    private String username;
    private String email;
    private String password;
    private String tipo_documento;
    private String dni;
    private String fecha_nacimiento; // ISO string, we'll parse if needed
    private String telefono;
    private String telefono_apoderado;
    private String direccion;
    private String notas;
    private String sexo;

    // Usuario specific
    private Long rol_id;
    private Boolean email_verificado;
    private String estado;
}
