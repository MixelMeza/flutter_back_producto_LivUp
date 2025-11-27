package edu.pe.residencias.model.dto;

public class MapResidenciaDTO {
    private Long id;
    private String nombre;
    private Double lat;
    private Double lng;
    private String tipo;

    public MapResidenciaDTO() {}

    public MapResidenciaDTO(Long id, String nombre, Double lat, Double lng, String tipo) {
        this.id = id;
        this.nombre = nombre;
        this.lat = lat;
        this.lng = lng;
        this.tipo = tipo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}
