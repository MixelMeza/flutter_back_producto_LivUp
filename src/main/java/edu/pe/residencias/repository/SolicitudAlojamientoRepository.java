package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.SolicitudAlojamiento;
import edu.pe.residencias.model.enums.SolicitudEstado;

@Repository
public interface SolicitudAlojamientoRepository extends JpaRepository<SolicitudAlojamiento, Long> {
	java.util.Optional<SolicitudAlojamiento> findByHabitacionIdAndEstudianteId(Long habitacionId, Long estudianteId);
	java.util.Optional<SolicitudAlojamiento> findByHabitacionIdAndEstudianteIdAndEstado(Long habitacionId, Long estudianteId, SolicitudEstado estado);
	java.util.List<SolicitudAlojamiento> findByHabitacionIdOrderByFechaSolicitudDesc(Long habitacionId);
}
