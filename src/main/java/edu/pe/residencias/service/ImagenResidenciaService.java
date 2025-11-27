package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import edu.pe.residencias.model.entity.ImagenResidencia;

public interface ImagenResidenciaService {
    ImagenResidencia create(ImagenResidencia imagenResidencia);
    ImagenResidencia update(ImagenResidencia imagenResidencia);
    void delete(Long id);
    Optional<ImagenResidencia> read(Long id);
    List<ImagenResidencia> readAll();
    List<ImagenResidencia> updateListForResidencia(Long residenciaId, java.util.List<String> urls);
}
