package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.Rol;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {
	java.util.Optional<Rol> findByNombre(String nombre);
}
