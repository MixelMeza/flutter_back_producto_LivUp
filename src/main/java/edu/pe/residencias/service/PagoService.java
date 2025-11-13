package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import edu.pe.residencias.model.entity.Pago;

public interface PagoService {
    Pago create(Pago pago);
    Pago update(Pago pago);
    void delete(Long id);
    Optional<Pago> read(Long id);
    List<Pago> readAll();
}
