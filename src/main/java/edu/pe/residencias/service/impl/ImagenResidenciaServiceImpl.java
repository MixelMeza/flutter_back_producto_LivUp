package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.ImagenResidencia;
import edu.pe.residencias.repository.ImagenResidenciaRepository;
import edu.pe.residencias.service.ImagenResidenciaService;

@Service
public class ImagenResidenciaServiceImpl implements ImagenResidenciaService {

    @Autowired
    private ImagenResidenciaRepository repository;

    @Override
    public ImagenResidencia create(ImagenResidencia imagenResidencia) {
        return repository.save(imagenResidencia);
    }

    @Override
    public ImagenResidencia update(ImagenResidencia imagenResidencia) {
        return repository.save(imagenResidencia);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<ImagenResidencia> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<ImagenResidencia> readAll() {
        return repository.findAll();
    }
}
