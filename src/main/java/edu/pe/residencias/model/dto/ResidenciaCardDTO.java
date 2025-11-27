package edu.pe.residencias.model.dto;

import java.math.BigDecimal;

public class ResidenciaCardDTO {
    private Long id;
    private String nombre;
    private String tipo;
    private String imagen_principal;
    private Double rating;
    private BigDecimal precio_desde;
    private Integer habitaciones_disponibles;
    private Integer habitaciones_totales;

    public ResidenciaCardDTO() {}

    public ResidenciaCardDTO(Long id, String nombre, String tipo, String imagen_principal, Double rating, BigDecimal precio_desde, Integer habitaciones_disponibles, Integer habitaciones_totales) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.imagen_principal = imagen_principal;
        this.rating = rating;
        this.precio_desde = precio_desde;
        this.habitaciones_disponibles = habitaciones_disponibles;
        this.habitaciones_totales = habitaciones_totales;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getImagen_principal() { return imagen_principal; }
    public void setImagen_principal(String imagen_principal) { this.imagen_principal = imagen_principal; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public BigDecimal getPrecio_desde() { return precio_desde; }
    public void setPrecio_desde(BigDecimal precio_desde) { this.precio_desde = precio_desde; }
    public Integer getHabitaciones_disponibles() { return habitaciones_disponibles; }
    public void setHabitaciones_disponibles(Integer habitaciones_disponibles) { this.habitaciones_disponibles = habitaciones_disponibles; }
    public Integer getHabitaciones_totales() { return habitaciones_totales; }
    public void setHabitaciones_totales(Integer habitaciones_totales) { this.habitaciones_totales = habitaciones_totales; }
}
