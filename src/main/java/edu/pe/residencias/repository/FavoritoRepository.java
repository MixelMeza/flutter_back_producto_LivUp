package edu.pe.residencias.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pe.residencias.model.entity.Favorito;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {
    Optional<Favorito> findByUsuarioIdAndHabitacionId(Long usuarioId, Long habitacionId);
    boolean existsByUsuarioIdAndHabitacionId(Long usuarioId, Long habitacionId);
    long countByHabitacionId(Long habitacionId);
    void deleteByUsuarioIdAndHabitacionId(Long usuarioId, Long habitacionId);
}
