package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.Ubicacion;
import edu.pe.residencias.repository.UbicacionRepository;
import edu.pe.residencias.service.UbicacionService;

@Service
public class UbicacionServiceImpl implements UbicacionService {

    @Autowired
    private UbicacionRepository repository;

    @Override
    public Ubicacion create(Ubicacion ubicacion) {
        return repository.save(ubicacion);
    }

    @Override
    public Ubicacion update(Ubicacion ubicacion) {
        return repository.save(ubicacion);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Ubicacion> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Ubicacion> readAll() {
        return repository.findAll();
    }
}
