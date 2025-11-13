package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.Contrato;
import edu.pe.residencias.repository.ContratoRepository;
import edu.pe.residencias.service.ContratoService;

@Service
public class ContratoServiceImpl implements ContratoService {

    @Autowired
    private ContratoRepository repository;

    @Override
    public Contrato create(Contrato contrato) {
        return repository.save(contrato);
    }

    @Override
    public Contrato update(Contrato contrato) {
        return repository.save(contrato);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Contrato> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Contrato> readAll() {
        return repository.findAll();
    }
}
