package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.Residencia;

@Repository
public interface ResidenciaRepository extends JpaRepository<Residencia, Long> {
}
