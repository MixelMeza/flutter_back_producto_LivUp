package edu.pe.residencias.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.Habitacion;
import edu.pe.residencias.model.enums.HabitacionEstado;

@Repository
public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {
	long countByResidenciaId(Long residenciaId);
	long countByResidenciaIdAndEstado(Long residenciaId, HabitacionEstado estado);
	List<Habitacion> findByResidenciaId(Long residenciaId);
	List<Habitacion> findByResidenciaIdAndEstado(Long residenciaId, HabitacionEstado estado);
}
