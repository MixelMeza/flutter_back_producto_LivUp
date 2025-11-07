package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.Rol;
import edu.pe.residencias.repository.RolRepository;
import edu.pe.residencias.service.RolService;

@Service
public class RolServiceImpl implements RolService {

    @Autowired
    private RolRepository repository;

    @Override
    public Rol create(Rol rol) {
        return repository.save(rol);
    }

    @Override
    public Rol update(Rol rol) {
        return repository.save(rol);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Rol> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Rol> readAll() {
        return repository.findAll();
    }
}
