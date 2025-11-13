package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import edu.pe.residencias.model.entity.Ubicacion;

public interface UbicacionService {
    Ubicacion create(Ubicacion ubicacion);
    Ubicacion update(Ubicacion ubicacion);
    void delete(Long id);
    Optional<Ubicacion> read(Long id);
    List<Ubicacion> readAll();
}
