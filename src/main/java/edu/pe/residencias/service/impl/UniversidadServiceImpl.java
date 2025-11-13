package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.Universidad;
import edu.pe.residencias.repository.UniversidadRepository;
import edu.pe.residencias.service.UniversidadService;

@Service
public class UniversidadServiceImpl implements UniversidadService {

    @Autowired
    private UniversidadRepository repository;

    @Override
    public Universidad create(Universidad universidad) {
        return repository.save(universidad);
    }

    @Override
    public Universidad update(Universidad universidad) {
        return repository.save(universidad);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Universidad> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Universidad> readAll() {
        return repository.findAll();
    }
}
