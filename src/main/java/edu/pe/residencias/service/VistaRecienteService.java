package edu.pe.residencias.service;

import java.util.List;

import edu.pe.residencias.model.entity.VistaReciente;

public interface VistaRecienteService {
    void recordViewForUser(Long usuarioId, Long habitacionId);
    void recordViewForSession(String sessionUuid, Long habitacionId);
    List<VistaReciente> getRecentForUser(Long usuarioId, int limit);
    List<VistaReciente> getRecentForSession(String sessionUuid, int limit);
    void clearRecentForUser(Long usuarioId);
}
