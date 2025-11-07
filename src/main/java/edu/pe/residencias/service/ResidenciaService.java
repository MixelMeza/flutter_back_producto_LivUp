package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import edu.pe.residencias.model.entity.Residencia;

public interface ResidenciaService {
    Residencia create(Residencia residencia);
    Residencia update(Residencia residencia);
    void delete(Long id);
    Optional<Residencia> read(Long id);
    List<Residencia> readAll();
}
