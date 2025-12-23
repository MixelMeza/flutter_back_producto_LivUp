package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.GastoResidencia;

@Repository
public interface GastoResidenciaRepository extends JpaRepository<GastoResidencia, Long> {
	java.util.List<GastoResidencia> findByResidenciaId(Long residenciaId);
	java.util.List<GastoResidencia> findByPeriodo(String periodo);
	java.util.List<GastoResidencia> findByEstadoPago(GastoResidencia.EstadoPago estadoPago);
	java.util.List<GastoResidencia> findByTipoGasto(GastoResidencia.TipoGasto tipoGasto);
	java.util.List<GastoResidencia> findByMetodoPago(GastoResidencia.MetodoPago metodoPago);
}
