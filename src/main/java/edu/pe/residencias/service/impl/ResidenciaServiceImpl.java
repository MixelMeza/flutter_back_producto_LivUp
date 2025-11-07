package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.Residencia;
import edu.pe.residencias.repository.ResidenciaRepository;
import edu.pe.residencias.service.ResidenciaService;

@Service
public class ResidenciaServiceImpl implements ResidenciaService {

    @Autowired
    private ResidenciaRepository repository;

    @Override
    public Residencia create(Residencia residencia) {
        return repository.save(residencia);
    }

    @Override
    public Residencia update(Residencia residencia) {
        return repository.save(residencia);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Residencia> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Residencia> readAll() {
        return repository.findAll();
    }
}
