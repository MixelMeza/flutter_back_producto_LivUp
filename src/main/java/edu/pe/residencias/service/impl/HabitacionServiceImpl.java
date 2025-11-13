package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.Habitacion;
import edu.pe.residencias.repository.HabitacionRepository;
import edu.pe.residencias.service.HabitacionService;

@Service
public class HabitacionServiceImpl implements HabitacionService {

    @Autowired
    private HabitacionRepository repository;

    @Override
    public Habitacion create(Habitacion habitacion) {
        return repository.save(habitacion);
    }

    @Override
    public Habitacion update(Habitacion habitacion) {
        return repository.save(habitacion);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Habitacion> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Habitacion> readAll() {
        return repository.findAll();
    }
}
