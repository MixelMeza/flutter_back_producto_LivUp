package edu.pe.residencias.model.dto;

public class ImagenSimpleDTO {
    private String url;
    private Integer orden;

    public ImagenSimpleDTO() {}

    public ImagenSimpleDTO(String url, Integer orden) {
        this.url = url;
        this.orden = orden;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
}
