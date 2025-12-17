package edu.pe.residencias.service;

import java.util.Optional;

import edu.pe.residencias.model.entity.Dispositivo;

public interface DispositivoService {
    Optional<Dispositivo> findByFcmToken(String fcmToken);
    Dispositivo registerOrUpdate(String fcmToken, String plataforma, String modelo, String osVersion, Long usuarioId);
    void deleteById(Long id);
}
