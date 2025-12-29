package edu.pe.residencias.repository;

import edu.pe.residencias.model.entity.analytics.VistaHabitacionSemana;
import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VistaHabitacionSemanaRepository extends JpaRepository<VistaHabitacionSemana, Long> {

    Optional<VistaHabitacionSemana> findByHabitacionIdAndWeekKey(Long habitacionId, String weekKey);

    @Modifying
    @Query("UPDATE VistaHabitacionSemana v SET v.totalVistas = v.totalVistas + 1 WHERE v.habitacion.id = :habitacionId AND v.weekKey = :weekKey")
    int increment(@Param("habitacionId") Long habitacionId, @Param("weekKey") String weekKey);

    @Query("SELECT v.habitacion.id FROM VistaHabitacionSemana v WHERE v.weekKey = :weekKey ORDER BY v.totalVistas DESC")
    List<Long> findTopHabitacionIds(@Param("weekKey") String weekKey, Pageable pageable);
}
