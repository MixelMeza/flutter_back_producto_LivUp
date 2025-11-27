package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.Habitacion;
import edu.pe.residencias.repository.HabitacionRepository;
import edu.pe.residencias.repository.ResidenciaRepository;
import edu.pe.residencias.service.HabitacionService;

@Service
public class HabitacionServiceImpl implements HabitacionService {

    @Autowired
    private HabitacionRepository repository;

    @Autowired
    private ResidenciaRepository residenciaRepository;

    @Override
    public Habitacion create(Habitacion habitacion) {
        // Todas las habitaciones se crean en estado disponible
        if (habitacion.getEstado() == null || habitacion.getEstado().isEmpty()) {
            habitacion.setEstado("disponible");
        }
        Habitacion h = repository.save(habitacion);
        // Recalculate cantidad_habitaciones for the residencia
        try {
            if (h.getResidencia() != null && h.getResidencia().getId() != null) {
                Long resId = h.getResidencia().getId();
                long cnt = repository.countByResidenciaId(resId);
                residenciaRepository.findById(resId).ifPresent(r -> {
                    r.setCantidadHabitaciones((int) cnt);
                    residenciaRepository.save(r);
                });
            }
        } catch (Exception ex) {
            // log but don't break creation
            System.err.println("[HabitacionServiceImpl] Failed to update residencia count: " + ex.getMessage());
        }
        return h;
    }

    @Override
    public Habitacion update(Habitacion habitacion) {
        return repository.save(habitacion);
    }

    @Override
    public void delete(Long id) {
        // Before delete, get residencia id to update count afterwards
        try {
            Long residenciaId = null;
            java.util.Optional<Habitacion> opt = repository.findById(id);
            if (opt.isPresent() && opt.get().getResidencia() != null) {
                residenciaId = opt.get().getResidencia().getId();
            }
            repository.deleteById(id);
            if (residenciaId != null) {
                long cnt = repository.countByResidenciaId(residenciaId);
                residenciaRepository.findById(residenciaId).ifPresent(r -> {
                    r.setCantidadHabitaciones((int) cnt);
                    residenciaRepository.save(r);
                });
            }
        } catch (Exception ex) {
            System.err.println("[HabitacionServiceImpl] Failed to delete habitacion or update count: " + ex.getMessage());
            // rethrow or ignore depending on desired behavior; keep it simple
            throw ex;
        }
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
