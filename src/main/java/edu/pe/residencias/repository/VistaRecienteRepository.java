package edu.pe.residencias.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.pe.residencias.model.entity.VistaReciente;

public interface VistaRecienteRepository extends JpaRepository<VistaReciente, Long> {
    Optional<VistaReciente> findByUsuarioIdAndHabitacionId(Long usuarioId, Long habitacionId);
    Optional<VistaReciente> findBySessionUuidAndHabitacionId(String sessionUuid, Long habitacionId);

    @Query("SELECT v FROM VistaReciente v WHERE v.usuario.id = :userId ORDER BY v.vistoEn DESC")
    List<VistaReciente> findRecentByUsuarioId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT v FROM VistaReciente v WHERE v.sessionUuid = :sessionUuid ORDER BY v.vistoEn DESC")
    List<VistaReciente> findRecentBySessionUuid(@Param("sessionUuid") String sessionUuid, Pageable pageable);

    void deleteByUsuarioIdAndHabitacionId(Long usuarioId, Long habitacionId);
}
