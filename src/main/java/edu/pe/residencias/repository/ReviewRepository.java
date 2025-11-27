package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.Review;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
	@Query("select avg(r.puntuacion) from Review r where r.residencia.id = :residenciaId")
	Double findAveragePuntuacionByResidenciaId(@Param("residenciaId") Long residenciaId);

	@Query("select count(r) from Review r where r.residencia.id = :residenciaId")
	Long countByResidenciaId(@Param("residenciaId") Long residenciaId);
}
