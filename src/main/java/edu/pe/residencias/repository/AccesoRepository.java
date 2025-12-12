package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.Acceso;

@Repository
public interface AccesoRepository extends JpaRepository<Acceso, Long> {
	java.util.Optional<Acceso> findFirstByUsuarioIdOrderByUltimaSesionDesc(Long usuarioId);

	// Find an acceso for a given user where the dispositivo string contains a fragment (useful for deviceId)
	java.util.Optional<Acceso> findFirstByUsuarioIdAndDispositivoContaining(Long usuarioId, String dispositivoFragment);

	// Find by exact dispositivo string for the user
	java.util.Optional<Acceso> findFirstByUsuarioIdAndDispositivo(Long usuarioId, String dispositivo);

	// Prefer lookup by device_id column when available
	java.util.Optional<Acceso> findFirstByUsuarioIdAndDeviceId(Long usuarioId, String deviceId);
}
