package edu.pe.residencias.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.pe.residencias.model.entity.Residencia;

@Repository
public interface ResidenciaRepository extends JpaRepository<Residencia, Long> {
	List<Residencia> findByUsuarioId(Long usuarioId);

	// Fetch residencias with their imagenes collection to avoid
	// LazyInitializationException
	@Query("select distinct r from Residencia r left join fetch r.imagenesResidencia i where r.usuario.id = :usuarioId")
	List<Residencia> findByUsuarioIdWithImagenes(@Param("usuarioId") Long usuarioId);

	// Fetch a single residencia with its imagenes initialized
	@Query("select r from Residencia r left join fetch r.imagenesResidencia i where r.id = :id")
	java.util.Optional<Residencia> findByIdWithImagenes(@Param("id") Long id);

	// Simple paginated search by nombre or descripcion (case-insensitive)
	Page<Residencia> findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCase(String nombre,
			String descripcion, Pageable pageable);

	// Paginated list of all residencias
	Page<Residencia> findAll(Pageable pageable);

	// Residencias whose residencia.estado is active and whose usuario has email verified and is active
	@Query("select r from Residencia r join r.usuario u join r.ubicacion ub where lower(r.estado) = 'activo' and u.emailVerificado = true and lower(u.estado) = 'activo'")
	java.util.List<Residencia> findAllActiveWithVerifiedUsuario();

	// Featured residencias: return only IDs (for lightweight mobile requests)
	@Query("select r.id from Residencia r join r.usuario u join r.ubicacion ub "
			+ "where lower(r.estado) = 'activo' and u.emailVerificado = true and lower(u.estado) = 'activo' "
			+ "and r.destacado = true "
			+ "order by case when r.destacadoFecha is null then 1 else 0 end, r.destacadoFecha desc, r.id desc")
	java.util.List<Long> findFeaturedResidenciaIds();
}
