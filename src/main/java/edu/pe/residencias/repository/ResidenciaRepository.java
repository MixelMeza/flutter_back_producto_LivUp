package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.pe.residencias.model.entity.Residencia;

@Repository
public interface ResidenciaRepository extends JpaRepository<Residencia, Long> {
	List<Residencia> findByUsuarioId(Long usuarioId);

	// Fetch residencias with their imagenes collection to avoid LazyInitializationException
	@Query("select distinct r from Residencia r left join fetch r.imagenesResidencia i where r.usuario.id = :usuarioId")
	List<Residencia> findByUsuarioIdWithImagenes(@Param("usuarioId") Long usuarioId);

	// Fetch a single residencia with its imagenes initialized
	@Query("select r from Residencia r left join fetch r.imagenesResidencia i where r.id = :id")
	java.util.Optional<Residencia> findByIdWithImagenes(@Param("id") Long id);

	// Paginado para admin - todas las residencias
	Page<Residencia> findAll(Pageable pageable);
}
