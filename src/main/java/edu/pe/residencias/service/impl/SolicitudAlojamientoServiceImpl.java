package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.SolicitudAlojamiento;
import edu.pe.residencias.model.enums.SolicitudEstado;
import edu.pe.residencias.repository.SolicitudAlojamientoRepository;
import edu.pe.residencias.service.SolicitudAlojamientoService;

@Service
public class SolicitudAlojamientoServiceImpl implements SolicitudAlojamientoService {

    @Autowired
    private SolicitudAlojamientoRepository repository;

    @Override
    public SolicitudAlojamiento create(SolicitudAlojamiento solicitudAlojamiento) {
        if (solicitudAlojamiento.getEstado() == null) {
            solicitudAlojamiento.setEstado(SolicitudEstado.PENDIENTE);
        }
        return repository.save(solicitudAlojamiento);
    }

    @Override
    public SolicitudAlojamiento update(SolicitudAlojamiento solicitudAlojamiento) {
        return repository.save(solicitudAlojamiento);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<SolicitudAlojamiento> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<SolicitudAlojamiento> readAll() {
        return repository.findAll();
    }
}
