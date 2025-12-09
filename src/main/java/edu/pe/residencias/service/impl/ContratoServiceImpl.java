package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.pe.residencias.model.entity.Contrato;
import edu.pe.residencias.model.dto.ContratoResumidoDTO;
import edu.pe.residencias.model.enums.ContratoEstado;
import edu.pe.residencias.model.enums.SolicitudEstado;
import edu.pe.residencias.repository.ContratoRepository;
import edu.pe.residencias.repository.ResidenciaRepository;
import edu.pe.residencias.service.ContratoService;
import edu.pe.residencias.service.NotificationService;

@Service
public class ContratoServiceImpl implements ContratoService {

    @Autowired
    private ContratoRepository repository;

    @Autowired
    private edu.pe.residencias.repository.SolicitudAlojamientoRepository solicitudAlojamientoRepository;
    
    @Autowired
    private ResidenciaRepository residenciaRepository;
    
    @Autowired
    private edu.pe.residencias.repository.HabitacionRepository habitacionRepository;
    
    @Autowired
    private NotificationService notificationService;

    @Override
    public Contrato create(Contrato contrato) {
        if (contrato.getEstado() == null) {
            // If fechaInicio is in the future, set to pendiente_inicio, otherwise vigente
            try {
                if (contrato.getFechaInicio() != null && contrato.getFechaInicio().isAfter(java.time.LocalDate.now())) {
                    contrato.setEstado(ContratoEstado.PENDIENTE_INICIO);
                } else {
                    contrato.setEstado(ContratoEstado.VIGENTE);
                }
            } catch (Exception ex) {
                contrato.setEstado(ContratoEstado.VIGENTE);
            }
        }
        
        // Cambiar estado de la solicitud asociada a 'aceptada'
        if (contrato.getSolicitud() != null && contrato.getSolicitud().getId() != null) {
            var solicitudOpt = solicitudAlojamientoRepository.findById(contrato.getSolicitud().getId());
            if (solicitudOpt.isPresent()) {
                var solicitud = solicitudOpt.get();
                solicitud.setEstado(SolicitudEstado.ACEPTADA);
                solicitudAlojamientoRepository.save(solicitud);
                
                // Cambiar estado de las demás solicitudes de la misma habitación a 'rechazada'
                if (solicitud.getHabitacion() != null && solicitud.getHabitacion().getId() != null) {
                    var otrasSolicitudes = solicitudAlojamientoRepository.findAll();
                    for (var otra : otrasSolicitudes) {
                        if (!otra.getId().equals(solicitud.getId()) &&
                                otra.getHabitacion() != null &&
                                otra.getHabitacion().getId().equals(solicitud.getHabitacion().getId()) &&
                                !SolicitudEstado.ACEPTADA.equals(otra.getEstado())) {
                            otra.setEstado(SolicitudEstado.RECHAZADA);
                            solicitudAlojamientoRepository.save(otra);
                        }
                    }
                }
            }
        }
        
        Contrato saved = repository.save(contrato);

        // Si el contrato está ligado a una solicitud/habitación, marcar la habitación como OCUPADO
        try {
            if (saved.getSolicitud() != null && saved.getSolicitud().getHabitacion() != null && saved.getSolicitud().getHabitacion().getId() != null) {
                var habOpt = habitacionRepository.findById(saved.getSolicitud().getHabitacion().getId());
                if (habOpt.isPresent()) {
                    var hab = habOpt.get();
                    hab.setEstado(edu.pe.residencias.model.enums.HabitacionEstado.OCUPADO);
                    habitacionRepository.save(hab);
                }
            }
        } catch (Exception e) {
            System.err.println("[CONTRATO] Error al marcar habitación como OCUPADO: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 🔔 NOTIFICACIÓN: CONTRATO_CREADO - Enviar al estudiante
        try {
            if (saved.getSolicitud() != null && saved.getSolicitud().getEstudiante() != null) {
                var estudiante = saved.getSolicitud().getEstudiante();
                
                // Obtener nombre de la residencia
                String nombreResidencia = "la residencia";
                if (saved.getSolicitud().getResidencia() != null && saved.getSolicitud().getResidencia().getId() != null) {
                    var residenciaOpt = residenciaRepository.findById(saved.getSolicitud().getResidencia().getId());
                    if (residenciaOpt.isPresent()) {
                        nombreResidencia = residenciaOpt.get().getNombre();
                    }
                }
                
                String titulo = "¡Contrato generado!";
                String mensaje = "Tu contrato para " + nombreResidencia + " está listo. Revisa los detalles y condiciones.";
                
                System.out.println("[NOTIFICACION] Enviando CONTRATO_CREADO al estudiante ID: " + estudiante.getId());
                
                notificationService.createNotification(
                    estudiante,
                    "CONTRATO_CREADO",
                    titulo,
                    mensaje,
                    "Contrato",
                    saved.getId()
                );
                
                System.out.println("[NOTIFICACION] Notificación CONTRATO_CREADO enviada exitosamente");
            }
        } catch (Exception e) {
            System.err.println("[NOTIFICACION] Error al enviar notificación CONTRATO_CREADO: " + e.getMessage());
            e.printStackTrace();
        }
        
        return saved;
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

    @Override
    public List<Contrato> findVigorosByResidenciaId(Long residenciaId) {
        return repository.findVigorosByResidenciaId(residenciaId);
    }

    @Override
    public Page<Contrato> findVigorosByResidenciaIdPaginated(Long residenciaId, Pageable pageable) {
        return repository.findVigorosByResidenciaIdPaginated(residenciaId, pageable);
    }

    @Override
    public List<ContratoResumidoDTO> mapToContratoResumidoDTOs(List<Contrato> contratos) {
        return contratos.stream().map(contrato -> {
            ContratoResumidoDTO dto = new ContratoResumidoDTO();
            dto.setId(contrato.getId());

            // Obtener datos del estudiante
            if (contrato.getSolicitud() != null && contrato.getSolicitud().getEstudiante() != null) {
                var estudiante = contrato.getSolicitud().getEstudiante();
                if (estudiante.getPersona() != null) {
                    dto.setEstudiante(
                            estudiante.getPersona().getNombre() + " " + estudiante.getPersona().getApellido());
                    dto.setEmail(estudiante.getPersona().getEmail());
                }
            }

            // Obtener datos de la habitación
            if (contrato.getSolicitud() != null && contrato.getSolicitud().getHabitacion() != null) {
                var habitacion = contrato.getSolicitud().getHabitacion();
                dto.setHabitacion(habitacion.getCodigoHabitacion() != null ? habitacion.getCodigoHabitacion()
                        : habitacion.getNombre());
            }

            dto.setFechaInicio(contrato.getFechaInicio());
            dto.setFechaFin(contrato.getFechaFin());
            dto.setFechaProximaRenovacion(contrato.getFechaProximaRenovacion());
            dto.setMontoTotal(contrato.getMontoTotal());
            dto.setEstado(contrato.getEstado());

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public Optional<Contrato> getContratoVigenteByUsuarioId(Long usuarioId) {
        return repository.findContratoVigenteByUsuarioId(usuarioId);
    }

    @Override
    public List<Contrato> getHistorialContratosByUsuarioId(Long usuarioId) {
        return repository.findHistorialContratosByUsuarioId(usuarioId);
    }
}
