package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import edu.pe.residencias.model.entity.Universidad;

public interface UniversidadService {
    Universidad create(Universidad universidad);
    Universidad update(Universidad universidad);
    void delete(Long id);
    Optional<Universidad> read(Long id);
    List<Universidad> readAll();
}
