package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import edu.pe.residencias.model.entity.Contrato;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {
	// count contracts for a given student (usuario) id
	long countBySolicitudEstudianteId(Long estudianteId);

	// count distinct habitaciones that have contracts for a given residencia and contrato estado
	@org.springframework.data.jpa.repository.Query("select count(distinct c.solicitud.habitacion.id) from Contrato c where c.solicitud.residencia.id = :residenciaId and lower(c.estado) = :estado")
	long countDistinctHabitacionesByResidenciaIdAndEstado(@org.springframework.data.repository.query.Param("residenciaId") Long residenciaId, @org.springframework.data.repository.query.Param("estado") String estado);

	// Find vigente contract for a given inquilino (usuario) id
	@org.springframework.data.jpa.repository.Query("SELECT c FROM Contrato c WHERE c.solicitud.estudiante.id = :usuarioId AND lower(c.estado) = 'vigente'")
	Optional<Contrato> findContratoVigenteByUsuarioId(@org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);
}
