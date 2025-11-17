package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.Pago;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
	@org.springframework.data.jpa.repository.Query("select coalesce(sum(p.monto), 0) from Pago p where p.contrato.solicitud.residencia.id = :residenciaId and lower(p.estado) in :estados")
	java.math.BigDecimal sumPagosByResidenciaIdAndEstados(@org.springframework.data.repository.query.Param("residenciaId") Long residenciaId, @org.springframework.data.repository.query.Param("estados") java.util.List<String> estados);
}
