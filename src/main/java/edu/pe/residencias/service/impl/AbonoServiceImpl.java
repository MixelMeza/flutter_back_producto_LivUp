package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.Abono;
import edu.pe.residencias.repository.AbonoRepository;
import edu.pe.residencias.service.AbonoService;

@Service
public class AbonoServiceImpl implements AbonoService {

    @Autowired
    private AbonoRepository repository;

    @Override
    public Abono create(Abono abono) {
        return repository.save(abono);
    }

    @Override
    public Optional<Abono> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Abono> readAll() {
        return repository.findAll();
    }

    @Override
    public Abono update(Abono abono) {
        return repository.save(abono);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
