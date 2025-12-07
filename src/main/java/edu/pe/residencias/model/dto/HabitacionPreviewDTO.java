package edu.pe.residencias.model.dto;

import java.math.BigDecimal;

public class HabitacionPreviewDTO {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private String estado;
    private String imagen_principal;
    private Integer piso;
    private Integer capacidad;
    private Boolean destacado;

    public HabitacionPreviewDTO() {}

    public HabitacionPreviewDTO(Long id, String nombre, BigDecimal precio, String estado, String imagen_principal) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.estado = estado;
        this.imagen_principal = imagen_principal;
    }

    public Integer getPiso() { return piso; }
    public void setPiso(Integer piso) { this.piso = piso; }
    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }
    public Boolean getDestacado() { return destacado; }
    public void setDestacado(Boolean destacado) { this.destacado = destacado; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getImagen_principal() { return imagen_principal; }
    public void setImagen_principal(String imagen_principal) { this.imagen_principal = imagen_principal; }
}
