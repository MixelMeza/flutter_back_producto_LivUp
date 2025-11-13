package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.Acceso;
import edu.pe.residencias.repository.AccesoRepository;
import edu.pe.residencias.service.AccesoService;

@Service
public class AccesoServiceImpl implements AccesoService {

    @Autowired
    private AccesoRepository repository;

    @Override
    public Acceso create(Acceso acceso) {
        return repository.save(acceso);
    }

    @Override
    public Acceso update(Acceso acceso) {
        return repository.save(acceso);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Acceso> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Acceso> readAll() {
        return repository.findAll();
    }
}
