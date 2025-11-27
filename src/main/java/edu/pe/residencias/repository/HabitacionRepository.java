package edu.pe.residencias.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.Habitacion;

@Repository
public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {
	long countByResidenciaId(Long residenciaId);
	List<Habitacion> findByResidenciaId(Long residenciaId);
}
