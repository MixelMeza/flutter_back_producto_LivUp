package edu.pe.residencias.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import edu.pe.residencias.model.enums.UsuarioEstado;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioAdminDTO {
    private Long id;
    private String username;
    private String email;
    private String nombre;
    private String apellido;
    private String rol; // propietario, inquilino, admin
    private UsuarioEstado estado; // activo, inactivo, suspendido
    private LocalDateTime createdAt;
    private Boolean emailVerificado;
}
