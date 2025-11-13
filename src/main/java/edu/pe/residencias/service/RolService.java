package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import edu.pe.residencias.model.entity.Rol;

public interface RolService {
    Rol create(Rol rol);
    Rol update(Rol rol);
    void delete(Long id);
    Optional<Rol> read(Long id);
    List<Rol> readAll();
}
