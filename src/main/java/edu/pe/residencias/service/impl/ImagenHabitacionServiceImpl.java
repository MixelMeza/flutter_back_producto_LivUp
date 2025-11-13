package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.ImagenHabitacion;
import edu.pe.residencias.repository.ImagenHabitacionRepository;
import edu.pe.residencias.service.ImagenHabitacionService;

@Service
public class ImagenHabitacionServiceImpl implements ImagenHabitacionService {

    @Autowired
    private ImagenHabitacionRepository repository;

    @Override
    public ImagenHabitacion create(ImagenHabitacion imagenHabitacion) {
        return repository.save(imagenHabitacion);
    }

    @Override
    public ImagenHabitacion update(ImagenHabitacion imagenHabitacion) {
        return repository.save(imagenHabitacion);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<ImagenHabitacion> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<ImagenHabitacion> readAll() {
        return repository.findAll();
    }
}
