package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import edu.pe.residencias.model.entity.SolicitudAlojamiento;

public interface SolicitudAlojamientoService {
    SolicitudAlojamiento create(SolicitudAlojamiento solicitudAlojamiento);
    SolicitudAlojamiento update(SolicitudAlojamiento solicitudAlojamiento);
    void delete(Long id);
    Optional<SolicitudAlojamiento> read(Long id);
    List<SolicitudAlojamiento> readAll();
}
