package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import edu.pe.residencias.model.entity.ImagenHabitacion;

public interface ImagenHabitacionService {
    ImagenHabitacion create(ImagenHabitacion imagenHabitacion);
    ImagenHabitacion update(ImagenHabitacion imagenHabitacion);
    void delete(Long id);
    Optional<ImagenHabitacion> read(Long id);
    List<ImagenHabitacion> readAll();
    List<ImagenHabitacion> updateListForHabitacion(Long habitacionId, java.util.List<String> urls);
}
