package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import edu.pe.residencias.model.entity.GastoResidencia;

public interface GastoResidenciaService {
    GastoResidencia create(GastoResidencia gastoResidencia);
    GastoResidencia update(GastoResidencia gastoResidencia);
    void delete(Long id);
    Optional<GastoResidencia> read(Long id);
    List<GastoResidencia> readAll();
}
