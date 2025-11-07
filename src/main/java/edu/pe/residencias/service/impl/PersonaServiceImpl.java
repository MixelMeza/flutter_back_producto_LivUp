package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.Persona;
import edu.pe.residencias.repository.PersonaRepository;
import edu.pe.residencias.service.PersonaService;

@Service
public class PersonaServiceImpl implements PersonaService {

    @Autowired
    private PersonaRepository repository;

    @Override
    public Persona create(Persona persona) {
        return repository.save(persona);
    }

    @Override
    public Persona update(Persona persona) {
        return repository.save(persona);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Persona> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Persona> readAll() {
        return repository.findAll();
    }
}
