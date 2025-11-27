package edu.pe.residencias.model.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import edu.pe.residencias.model.enums.UsuarioEstado;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String uuid;
    private String username;
    private String displayName;
    private String rol;
    private Long rol_id;
    private String email;
    private Boolean email_verificado;
    private String foto_url;
    private String telefono;
    private String ubicacion;
    private String direccion;
    private String fecha_nacimiento;
    private String created_at;
    private UsuarioEstado estado;
    private List<String> permisos;

    // stats
    private Integer n_contratos;
    private Integer n_abonos;
    private BigDecimal saldo_abonado;
    private String ultima_actividad;
}
