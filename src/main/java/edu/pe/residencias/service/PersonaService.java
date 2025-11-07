package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import edu.pe.residencias.model.entity.Persona;

public interface PersonaService {
    Persona create(Persona persona);
    Persona update(Persona persona);
    void delete(Long id);
    Optional<Persona> read(Long id);
    List<Persona> readAll();
}
