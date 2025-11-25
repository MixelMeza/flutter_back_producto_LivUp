package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.pe.residencias.model.entity.Residencia;
import edu.pe.residencias.model.dto.ResidenciaAdminDTO;
import edu.pe.residencias.repository.ResidenciaRepository;
import edu.pe.residencias.repository.HabitacionRepository;
import edu.pe.residencias.repository.ContratoRepository;
import edu.pe.residencias.service.ResidenciaService;

@Service
public class ResidenciaServiceImpl implements ResidenciaService {

    @Autowired
    private ResidenciaRepository repository;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private ContratoRepository contratoRepository;

    @Override
    public Residencia create(Residencia residencia) {
        if (residencia.getEstado() == null || residencia.getEstado().isEmpty()) {
            residencia.setEstado("activo");
        }
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

    @Override
    public Page<Residencia> findAllPaginated(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public List<ResidenciaAdminDTO> mapToResidenciaAdminDTOs(List<Residencia> residencias) {
        return residencias.stream().map(residencia -> {
            ResidenciaAdminDTO dto = new ResidenciaAdminDTO();
            dto.setId(residencia.getId());
            dto.setNombre(residencia.getNombre());
            dto.setTipo(residencia.getTipo());
            
            // Ubicación
            if (residencia.getUbicacion() != null) {
                dto.setUbicacion(residencia.getUbicacion().getDireccion() + ", " + residencia.getUbicacion().getDistrito());
            }
            
            // Propietario
            if (residencia.getUsuario() != null && residencia.getUsuario().getPersona() != null) {
                dto.setPropietario(residencia.getUsuario().getPersona().getNombre() + " " + residencia.getUsuario().getPersona().getApellido());
            }
            
            dto.setCantidadHabitaciones(residencia.getCantidadHabitaciones());
            
            // Habitaciones ocupadas
            long ocupadas = contratoRepository.countDistinctHabitacionesByResidenciaIdAndEstado(residencia.getId(), "vigente");
            dto.setHabitacionesOcupadas((int) ocupadas);
            
            dto.setEstado(residencia.getEstado());
            dto.setEmailContacto(residencia.getEmailContacto());
            dto.setTelefonoContacto(residencia.getTelefonoContacto());
            
            return dto;
        }).collect(Collectors.toList());
    }
}
