package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.pe.residencias.model.entity.Usuario;
import java.lang.Long;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
	java.util.Optional<Usuario> findByUsername(String username);
	java.util.Optional<Usuario> findByPersonaEmail(String email);

	java.util.Optional<Usuario> findByUuid(String uuid);

	// Paginado para admin - todos los usuarios
	Page<Usuario> findAll(Pageable pageable);
}
