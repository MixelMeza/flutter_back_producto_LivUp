package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.pe.residencias.model.entity.Residencia;
import edu.pe.residencias.model.dto.ResidenciaAdminDTO;

public interface ResidenciaService {
    Residencia create(Residencia residencia);

    Residencia update(Residencia residencia);

    void delete(Long id);

    Optional<Residencia> read(Long id);

    List<Residencia> readAll();

    // Métodos para admin - paginación
    Page<Residencia> findAllPaginated(Pageable pageable);

    List<ResidenciaAdminDTO> mapToResidenciaAdminDTOs(List<Residencia> residencias);

        java.util.List<edu.pe.residencias.model.dto.MapResidenciaDTO> findForMap();

    // Paginated search; if q is null or empty, returns all residencias paginated
    Page<Residencia> search(String q, Pageable pageable);
    edu.pe.residencias.model.dto.ResidenciaCardDTO getCardById(Long id);

}
