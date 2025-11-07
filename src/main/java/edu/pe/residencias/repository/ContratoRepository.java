package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.Contrato;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {
}
