
package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import edu.pe.residencias.model.entity.Contrato;
import java.util.List;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {
	// count contracts for a given student (usuario) id
	long countBySolicitudEstudianteId(Long estudianteId);

	// count distinct habitaciones that have contracts for a given residencia and contrato estado
	@Query("select count(distinct c.solicitud.habitacion.id) from Contrato c where c.solicitud.residencia.id = :residenciaId and lower(c.estado) = :estado")
	long countDistinctHabitacionesByResidenciaIdAndEstado(@Param("residenciaId") Long residenciaId, @Param("estado") String estado);

	// Obtener contratos vigentes por residencia (para propietario)
	@Query("SELECT c FROM Contrato c WHERE c.solicitud.residencia.id = :residenciaId AND lower(c.estado) = 'vigente' ORDER BY c.fechaInicio DESC")
	List<Contrato> findVigorosByResidenciaId(@Param("residenciaId") Long residenciaId);

	// Obtener contratos vigentes paginados por residencia
	@Query("SELECT c FROM Contrato c WHERE c.solicitud.residencia.id = :residenciaId AND lower(c.estado) = 'vigente' ORDER BY c.fechaInicio DESC")
	Page<Contrato> findVigorosByResidenciaIdPaginated(@Param("residenciaId") Long residenciaId, Pageable pageable);

	// Find vigente contract for a given inquilino (usuario) id
	@Query("SELECT c FROM Contrato c WHERE c.solicitud.estudiante.id = :usuarioId AND lower(c.estado) = 'vigente'")
	Optional<Contrato> findContratoVigenteByUsuarioId(@Param("usuarioId") Long usuarioId);

	// Find all contracts for a given inquilino (usuario) id except those with estado 'vigente'
	@Query("SELECT c FROM Contrato c WHERE c.solicitud.estudiante.id = :usuarioId AND lower(c.estado) <> 'vigente'")
	List<Contrato> findHistorialContratosByUsuarioId(@Param("usuarioId") Long usuarioId);
}
