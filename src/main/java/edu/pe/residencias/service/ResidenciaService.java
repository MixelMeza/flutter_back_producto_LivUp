package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.pe.residencias.model.entity.Residencia;

public interface ResidenciaService {
    Residencia create(Residencia residencia);
    Residencia update(Residencia residencia);
    void delete(Long id);
    Optional<Residencia> read(Long id);
    List<Residencia> readAll();

    // Paginated search; if q is null or empty, returns all residencias paginated
    Page<Residencia> search(String q, Pageable pageable);
}
