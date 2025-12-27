package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.Persona;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {
	java.util.Optional<Persona> findByDni(String dni);
	java.util.Optional<Persona> findByEmail(String email);
	java.util.Optional<Persona> findByTelefono(String telefono);
}
