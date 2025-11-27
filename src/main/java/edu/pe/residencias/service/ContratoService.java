package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.pe.residencias.model.entity.Contrato;
import edu.pe.residencias.model.dto.ContratoResumidoDTO;

public interface ContratoService {
    Contrato create(Contrato contrato);

    Contrato update(Contrato contrato);

    void delete(Long id);

    Optional<Contrato> read(Long id);

    List<Contrato> readAll();

    // Métodos para propietario - contratos vigentes por residencia
    List<Contrato> findVigorosByResidenciaId(Long residenciaId);

    Page<Contrato> findVigorosByResidenciaIdPaginated(Long residenciaId, Pageable pageable);

    List<ContratoResumidoDTO> mapToContratoResumidoDTOs(List<Contrato> contratos);

    // Métodos para estudiante - historial de contratos
    Optional<Contrato> getContratoVigenteByUsuarioId(Long usuarioId);

    List<Contrato> getHistorialContratosByUsuarioId(Long usuarioId);
}
