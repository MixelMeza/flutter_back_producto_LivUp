package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.ImagenResidencia;

@Repository
public interface ImagenResidenciaRepository extends JpaRepository<ImagenResidencia, Long> {
	java.util.List<ImagenResidencia> findByResidenciaId(Long residenciaId);
}
