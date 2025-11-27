package edu.pe.residencias.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import edu.pe.residencias.model.entity.Favorito;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {
    Optional<Favorito> findByUsuarioIdAndHabitacionId(Long usuarioId, Long habitacionId);
    boolean existsByUsuarioIdAndHabitacionId(Long usuarioId, Long habitacionId);
    long countByHabitacionId(Long habitacionId);
    void deleteByUsuarioIdAndHabitacionId(Long usuarioId, Long habitacionId);
    @Query("SELECT f FROM Favorito f JOIN FETCH f.usuario JOIN FETCH f.habitacion")
    java.util.List<Favorito> findAllWithUsuarioAndHabitacion();
    
    @Query("SELECT DISTINCT f FROM Favorito f " +
           "JOIN FETCH f.habitacion h " +
           "LEFT JOIN FETCH h.imagenesHabitacion " +
           "LEFT JOIN FETCH h.residencia r " +
           "LEFT JOIN FETCH r.ubicacion " +
           "WHERE f.usuario.id = :usuarioId " +
           "ORDER BY f.fecha DESC")
    java.util.List<Favorito> findByUsuarioIdWithHabitacionAndResidencia(@org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);
}
