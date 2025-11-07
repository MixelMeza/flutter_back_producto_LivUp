package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import edu.pe.residencias.model.entity.Acceso;

public interface AccesoService {
    Acceso create(Acceso acceso);
    Acceso update(Acceso acceso);
    void delete(Long id);
    Optional<Acceso> read(Long id);
    List<Acceso> readAll();
}
