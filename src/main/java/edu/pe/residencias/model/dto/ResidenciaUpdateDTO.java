package edu.pe.residencias.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResidenciaUpdateDTO {
    private String nombre;
    private String tipo;
    private String reglamentoUrl;
    private String descripcion;
    private String telefonoContacto;
    private String emailContacto;
    private String servicios;
}
