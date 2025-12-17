package edu.pe.residencias.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.pe.residencias.model.entity.Dispositivo;
import edu.pe.residencias.model.entity.Usuario;
import edu.pe.residencias.repository.DispositivoRepository;
import edu.pe.residencias.repository.UsuarioRepository;
import edu.pe.residencias.service.DispositivoService;

@Service
public class DispositivoServiceImpl implements DispositivoService {

    @Autowired
    private DispositivoRepository dispositivoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public Optional<Dispositivo> findByFcmToken(String fcmToken) {
        if (fcmToken == null) return Optional.empty();
        return dispositivoRepository.findByFcmToken(fcmToken);
    }

    @Override
    @Transactional
    public Dispositivo registerOrUpdate(String fcmToken, String plataforma, String modelo, String osVersion, Long usuarioId) {
        if (fcmToken == null || fcmToken.isBlank()) throw new IllegalArgumentException("fcmToken is required");

        Optional<Dispositivo> existingOpt = dispositivoRepository.findByFcmToken(fcmToken);
        Dispositivo d;
        if (existingOpt.isPresent()) {
            d = existingOpt.get();
            d.setPlataforma(plataforma);
            d.setModelo(modelo);
            d.setOsVersion(osVersion);
            // update and (re)assign usuario if provided
            if (usuarioId != null) {
                usuarioRepository.findById(usuarioId).ifPresent(d::setUsuario);
            }
        } else {
            d = new Dispositivo();
            d.setFcmToken(fcmToken);
            d.setPlataforma(plataforma);
            d.setModelo(modelo);
            d.setOsVersion(osVersion);
            if (usuarioId != null) usuarioRepository.findById(usuarioId).ifPresent(d::setUsuario);
        }
        return dispositivoRepository.save(d);
    }

    @Override
    public void deleteById(Long id) {
        dispositivoRepository.deleteById(id);
    }
}
