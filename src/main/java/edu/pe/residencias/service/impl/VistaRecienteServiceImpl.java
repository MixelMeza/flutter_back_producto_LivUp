package edu.pe.residencias.service.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.pe.residencias.model.entity.Habitacion;
import edu.pe.residencias.model.entity.Usuario;
import edu.pe.residencias.model.entity.VistaReciente;
import edu.pe.residencias.repository.HabitacionRepository;
import edu.pe.residencias.repository.UsuarioRepository;
import edu.pe.residencias.repository.VistaRecienteRepository;
import edu.pe.residencias.service.VistaRecienteService;

@Service
public class VistaRecienteServiceImpl implements VistaRecienteService {

    @Autowired
    private VistaRecienteRepository vistaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Override
    @Transactional
    public void recordViewForUser(Long usuarioId, Long habitacionId) {
        if (usuarioId == null || habitacionId == null) return;
        Habitacion h = habitacionRepository.findById(habitacionId).orElse(null);
        Usuario u = usuarioRepository.findById(usuarioId).orElse(null);
        if (h == null || u == null) return;

        vistaRepository.findByUsuarioIdAndHabitacionId(usuarioId, habitacionId).ifPresentOrElse(v -> {
            v.setVistoEn(Instant.now());
            vistaRepository.save(v);
        }, () -> {
            VistaReciente v = new VistaReciente();
            v.setUsuario(u);
            v.setHabitacion(h);
            v.setVistoEn(Instant.now());
            vistaRepository.save(v);
        });

        // Optional: trim to last N entries (not implemented here)
    }

    @Override
    @Transactional
    public void recordViewForSession(String sessionUuid, Long habitacionId) {
        if (sessionUuid == null || habitacionId == null) return;
        Habitacion h = habitacionRepository.findById(habitacionId).orElse(null);
        if (h == null) return;

        vistaRepository.findBySessionUuidAndHabitacionId(sessionUuid, habitacionId).ifPresentOrElse(v -> {
            v.setVistoEn(Instant.now());
            vistaRepository.save(v);
        }, () -> {
            VistaReciente v = new VistaReciente();
            v.setSessionUuid(sessionUuid);
            v.setHabitacion(h);
            v.setVistoEn(Instant.now());
            vistaRepository.save(v);
        });
    }

    @Override
    public List<VistaReciente> getRecentForUser(Long usuarioId, int limit) {
        if (usuarioId == null) return java.util.Collections.emptyList();
        return vistaRepository.findRecentByUsuarioId(usuarioId, PageRequest.of(0, limit));
    }

    @Override
    public List<VistaReciente> getRecentForSession(String sessionUuid, int limit) {
        if (sessionUuid == null) return java.util.Collections.emptyList();
        return vistaRepository.findRecentBySessionUuid(sessionUuid, PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public void clearRecentForUser(Long usuarioId) {
        if (usuarioId == null) return;
        // Delete all VistaReciente for user
        List<VistaReciente> list = vistaRepository.findRecentByUsuarioId(usuarioId, PageRequest.of(0, Integer.MAX_VALUE));
        vistaRepository.deleteAll(list);
    }
}
