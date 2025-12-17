package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.Acceso;

@Repository
public interface AccesoRepository extends JpaRepository<Acceso, Long> {
	java.util.Optional<Acceso> findFirstByUsuarioIdOrderByUltimaSesionDesc(Long usuarioId);

	// Lookup by related Dispositivo (new model)
	java.util.Optional<Acceso> findFirstByUsuarioIdAndDispositivoRelId(Long usuarioId, Long dispositivoId);

	// Lookup by dispositivo fcm token via relation
	java.util.Optional<Acceso> findFirstByUsuarioIdAndDispositivoRelFcmToken(Long usuarioId, String fcmToken);

	// Find the most recent Acceso for a given dispositivo (to determine who is currently logged in on that device)
	java.util.Optional<Acceso> findFirstByDispositivoRelIdOrderByUltimaSesionDesc(Long dispositivoId);
}
