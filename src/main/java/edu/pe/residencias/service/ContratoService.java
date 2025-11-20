package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import edu.pe.residencias.model.entity.Contrato;

public interface ContratoService {
    Contrato create(Contrato contrato);
    Contrato update(Contrato contrato);
    void delete(Long id);
    Optional<Contrato> read(Long id);
    List<Contrato> readAll();

    Optional<Contrato> getContratoVigenteByUsuarioId(Long usuarioId);

    List<Contrato> getHistorialContratosByUsuarioId(Long usuarioId);
}
