package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.ImagenHabitacion;

@Repository
public interface ImagenHabitacionRepository extends JpaRepository<ImagenHabitacion, Long> {
	java.util.List<ImagenHabitacion> findByHabitacionId(Long habitacionId);
}
