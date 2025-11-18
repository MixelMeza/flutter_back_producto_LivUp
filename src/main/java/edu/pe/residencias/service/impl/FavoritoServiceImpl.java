package edu.pe.residencias.service.impl;

import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.pe.residencias.model.entity.Favorito;
import edu.pe.residencias.model.entity.Habitacion;
import edu.pe.residencias.model.entity.Usuario;
import edu.pe.residencias.repository.FavoritoRepository;
import edu.pe.residencias.repository.HabitacionRepository;
import edu.pe.residencias.repository.UsuarioRepository;
import edu.pe.residencias.service.FavoritoService;

@Service
public class FavoritoServiceImpl implements FavoritoService {

    @Autowired
    private FavoritoRepository favoritoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Override
    @Transactional
    public void like(Long usuarioId, Long habitacionId) {
        if (usuarioId == null || habitacionId == null) return;
        Optional<Favorito> opt = favoritoRepository.findByUsuarioIdAndHabitacionId(usuarioId, habitacionId);
        if (opt.isPresent()) return; // idempotent

        Usuario u = usuarioRepository.findById(usuarioId).orElse(null);
        Habitacion h = habitacionRepository.findById(habitacionId).orElse(null);
        if (u == null || h == null) return;

        Favorito f = new Favorito();
        f.setUsuario(u);
        f.setHabitacion(h);
        f.setFecha(Instant.now());
        favoritoRepository.save(f);
    }

    @Override
    @Transactional
    public void unlike(Long usuarioId, Long habitacionId) {
        if (usuarioId == null || habitacionId == null) return;
        favoritoRepository.deleteByUsuarioIdAndHabitacionId(usuarioId, habitacionId);
    }

    @Override
    public boolean isLiked(Long usuarioId, Long habitacionId) {
        if (usuarioId == null || habitacionId == null) return false;
        return favoritoRepository.existsByUsuarioIdAndHabitacionId(usuarioId, habitacionId);
    }

    @Override
    public long countLikes(Long habitacionId) {
        if (habitacionId == null) return 0L;
        return favoritoRepository.countByHabitacionId(habitacionId);
    }

    @Override
    public java.util.List<Favorito> findAll() {
        return favoritoRepository.findAllWithUsuarioAndHabitacion();
    }
        @Override
        public Favorito save(Favorito favorito) {
            return favoritoRepository.save(favorito);
        }

        @Override
        public Favorito findById(Long id) {
            return favoritoRepository.findById(id).orElse(null);
        }

        @Override
        public void deleteById(Long id) {
            favoritoRepository.deleteById(id);
        }
}
