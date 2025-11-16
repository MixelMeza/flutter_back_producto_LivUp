package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.Acceso;

@Repository
public interface AccesoRepository extends JpaRepository<Acceso, Long> {
	java.util.Optional<Acceso> findFirstByUsuarioIdOrderByUltimaSesionDesc(Long usuarioId);
}
