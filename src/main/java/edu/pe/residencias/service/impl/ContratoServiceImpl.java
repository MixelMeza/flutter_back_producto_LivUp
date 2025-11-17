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

    @Autowired
    private edu.pe.residencias.repository.SolicitudAlojamientoRepository solicitudAlojamientoRepository;

    @Override
    public Contrato create(Contrato contrato) {
        if (contrato.getEstado() == null || contrato.getEstado().isEmpty()) {
            contrato.setEstado("vigente");
        }
        // Cambiar estado de la solicitud asociada a 'aceptada'
        if (contrato.getSolicitud() != null && contrato.getSolicitud().getId() != null) {
            var solicitudOpt = solicitudAlojamientoRepository.findById(contrato.getSolicitud().getId());
            if (solicitudOpt.isPresent()) {
                var solicitud = solicitudOpt.get();
                solicitud.setEstado("aceptada");
                solicitudAlojamientoRepository.save(solicitud);
                // Cambiar estado de las demás solicitudes de la misma habitación a 'ocupada'
                if (solicitud.getHabitacion() != null && solicitud.getHabitacion().getId() != null) {
                    var otrasSolicitudes = solicitudAlojamientoRepository.findAll();
                    for (var otra : otrasSolicitudes) {
                        if (!otra.getId().equals(solicitud.getId()) &&
                            otra.getHabitacion() != null &&
                            otra.getHabitacion().getId().equals(solicitud.getHabitacion().getId()) &&
                            !"aceptada".equals(otra.getEstado())) {
                            otra.setEstado("ocupada");
                            solicitudAlojamientoRepository.save(otra);
                        }
                    }
                }
            }
        }
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
