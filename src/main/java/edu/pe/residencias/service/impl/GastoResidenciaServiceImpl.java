package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.GastoResidencia;
import edu.pe.residencias.repository.GastoResidenciaRepository;
import edu.pe.residencias.service.GastoResidenciaService;

@Service
public class GastoResidenciaServiceImpl implements GastoResidenciaService {

    @Autowired
    private GastoResidenciaRepository repository;

    @Override
    public GastoResidencia create(GastoResidencia gastoResidencia) {
        return repository.save(gastoResidencia);
    }

    @Override
    public GastoResidencia update(GastoResidencia gastoResidencia) {
        return repository.save(gastoResidencia);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<GastoResidencia> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<GastoResidencia> readAll() {
        return repository.findAll();
    }
}
