package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.Pago;
import edu.pe.residencias.repository.PagoRepository;
import edu.pe.residencias.service.PagoService;

@Service
public class PagoServiceImpl implements PagoService {

    @Autowired
    private PagoRepository repository;

    @Override
    public Pago create(Pago pago) {
        return repository.save(pago);
    }

    @Override
    public Pago update(Pago pago) {
        return repository.save(pago);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Pago> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Pago> readAll() {
        return repository.findAll();
    }
}
