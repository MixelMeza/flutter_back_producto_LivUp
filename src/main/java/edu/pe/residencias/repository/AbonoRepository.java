package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.Abono;

@Repository
public interface AbonoRepository extends JpaRepository<Abono, Long> {
	long countByPagoContratoSolicitudEstudianteId(Long estudianteId);

	@org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(a.montoAbonado),0) FROM Abono a WHERE a.pago.contrato.solicitud.estudiante.id = :estudianteId")
	java.math.BigDecimal sumMontoByEstudianteId(@org.springframework.data.repository.query.Param("estudianteId") Long estudianteId);
}
