package edu.pe.residencias.repository;

import edu.pe.residencias.model.entity.analytics.FavoritoHabitacionSemana;
import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoritoHabitacionSemanaRepository extends JpaRepository<FavoritoHabitacionSemana, Long> {

    Optional<FavoritoHabitacionSemana> findByHabitacionIdAndWeekKey(Long habitacionId, String weekKey);

    @Modifying
    @Query("UPDATE FavoritoHabitacionSemana f SET f.totalFavoritos = f.totalFavoritos + 1 WHERE f.habitacion.id = :habitacionId AND f.weekKey = :weekKey")
    int increment(@Param("habitacionId") Long habitacionId, @Param("weekKey") String weekKey);

    @Query("SELECT f.habitacion.id FROM FavoritoHabitacionSemana f WHERE f.weekKey = :weekKey ORDER BY f.totalFavoritos DESC")
    List<Long> findTopHabitacionIds(@Param("weekKey") String weekKey, Pageable pageable);
}
