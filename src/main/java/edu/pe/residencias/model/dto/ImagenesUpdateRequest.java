package edu.pe.residencias.model.dto;

import java.util.List;

public class ImagenesUpdateRequest {
    private List<String> imagenes;

    public List<String> getImagenes() {
        return imagenes;
    }

    public void setImagenes(List<String> imagenes) {
        this.imagenes = imagenes;
    }
}
