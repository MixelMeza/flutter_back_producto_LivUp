package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import edu.pe.residencias.model.entity.Abono;

public interface AbonoService {
    Abono create(Abono abono);
    Optional<Abono> read(Long id);
    List<Abono> readAll();
    Abono update(Abono abono);
    void delete(Long id);
}
