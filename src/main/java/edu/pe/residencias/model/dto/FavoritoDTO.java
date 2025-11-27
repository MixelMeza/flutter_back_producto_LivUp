package edu.pe.residencias.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FavoritoDTO {
    private Long favoritoId;
    private Long habitacionId;
    private String habitacionNumero;
    private String habitacionTipo;
    private java.math.BigDecimal habitacionPrecio;
    private Long residenciaId;
    private String residenciaNombre;
    private String residenciaDireccion;
    private String imagenUrl;
    private String fechaAgregado;
}
