package edu.pe.residencias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.SolicitudAlojamiento;

@Repository
public interface SolicitudAlojamientoRepository extends JpaRepository<SolicitudAlojamiento, Long> {
}
