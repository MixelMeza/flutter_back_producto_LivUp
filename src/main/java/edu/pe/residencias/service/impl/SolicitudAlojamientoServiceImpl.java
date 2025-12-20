package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.SolicitudAlojamiento;
import edu.pe.residencias.model.entity.Residencia;
import edu.pe.residencias.model.entity.Habitacion;
import edu.pe.residencias.model.enums.HabitacionEstado;
import edu.pe.residencias.model.enums.SolicitudEstado;
import edu.pe.residencias.repository.SolicitudAlojamientoRepository;
import edu.pe.residencias.repository.ResidenciaRepository;
import edu.pe.residencias.service.SolicitudAlojamientoService;
import edu.pe.residencias.service.NotificationService;

@Service
public class SolicitudAlojamientoServiceImpl implements SolicitudAlojamientoService {

    @Autowired
    private SolicitudAlojamientoRepository repository;
    
    @Autowired
    private ResidenciaRepository residenciaRepository;
    
    @Autowired
    private edu.pe.residencias.repository.HabitacionRepository habitacionRepository;
    
    @Autowired
    private NotificationService notificationService;

    @Override
    public SolicitudAlojamiento create(SolicitudAlojamiento solicitudAlojamiento) {
        // Force initial state to PENDIENTE (created by tenant)
        solicitudAlojamiento.setEstado(SolicitudEstado.PENDIENTE);

        // If a habitacion is provided, validate it exists and is DISPONIBLE
        if (solicitudAlojamiento.getHabitacion() != null && solicitudAlojamiento.getHabitacion().getId() != null) {
            // Check if the same student already has a pending solicitud for this habitacion
            if (solicitudAlojamiento.getEstudiante() != null && solicitudAlojamiento.getEstudiante().getId() != null) {
                var existing = repository.findByHabitacionIdAndEstudianteIdAndEstado(
                    solicitudAlojamiento.getHabitacion().getId(),
                    solicitudAlojamiento.getEstudiante().getId(),
                    SolicitudEstado.PENDIENTE
                );
                if (existing.isPresent()) {
                    throw new IllegalStateException("Ya existe una solicitud pendiente para esta habitación");
                }
            }
            Long habId = solicitudAlojamiento.getHabitacion().getId();
            Habitacion hab = habitacionRepository.findById(habId).orElse(null);
            if (hab == null) {
                throw new IllegalArgumentException("Habitación no encontrada");
            }
            if (hab.getEstado() == null || !HabitacionEstado.DISPONIBLE.equals(hab.getEstado())) {
                throw new IllegalStateException("La habitación no está disponible");
            }
            // Do NOT modify the habitacion state here. Reservation is done by owner action.
        }

        // Ensure fechaSolicitud is set by server time (prevent client from spoofing)
        solicitudAlojamiento.setFechaSolicitud(edu.pe.residencias.util.DateTimeUtil.nowLima());
        SolicitudAlojamiento saved = repository.save(solicitudAlojamiento);
        
        // Enviar notificación al propietario de la residencia
        try {
            // Cargar la residencia completa con el propietario desde la BD
            if (saved.getResidencia() != null && saved.getResidencia().getId() != null) {
                Optional<Residencia> residenciaOpt = residenciaRepository.findById(saved.getResidencia().getId());
                
                if (residenciaOpt.isPresent()) {
                    Residencia residencia = residenciaOpt.get();
                    
                    if (residencia.getUsuario() != null) {
                        var propietario = residencia.getUsuario();
                        String titulo = "Nueva solicitud de alojamiento";
                        String mensaje = "Tienes una nueva solicitud para " + residencia.getNombre();
                        
                        System.out.println("[NOTIFICACION] Enviando notificación al propietario ID: " + propietario.getId());
                        
                        notificationService.createNotification(
                            propietario,
                            "SOLICITUD_NUEVA",
                            titulo,
                            mensaje,
                            "SolicitudAlojamiento",
                            saved.getId()
                        );
                        
                        System.out.println("[NOTIFICACION] Notificación enviada exitosamente");
                    } else {
                        System.err.println("[NOTIFICACION] La residencia no tiene propietario asignado");
                    }
                } else {
                    System.err.println("[NOTIFICACION] No se encontró la residencia con ID: " + saved.getResidencia().getId());
                }
            } else {
                System.err.println("[NOTIFICACION] La solicitud no tiene residencia asignada");
            }
        } catch (Exception e) {
            System.err.println("[NOTIFICACION] Error al enviar notificación: " + e.getMessage());
            e.printStackTrace();
        }
        
        return saved;
    }

    @Override
    public SolicitudAlojamiento update(SolicitudAlojamiento solicitudAlojamiento) {
        // Obtener estado anterior para detectar cambios
        Optional<SolicitudAlojamiento> existente = repository.findById(solicitudAlojamiento.getId());
        SolicitudEstado estadoAnterior = existente.map(SolicitudAlojamiento::getEstado).orElse(null);
        
        SolicitudAlojamiento updated = repository.save(solicitudAlojamiento);

        // Si el estado cambió a RESERVADA o ACEPTADA, actualizar el estado de la habitación correspondiente
        try {
            if (estadoAnterior != null && !estadoAnterior.equals(updated.getEstado()) && updated.getHabitacion() != null && updated.getHabitacion().getId() != null) {
                var habOpt = habitacionRepository.findById(updated.getHabitacion().getId());
                if (habOpt.isPresent()) {
                    var hab = habOpt.get();
                    if (updated.getEstado() == SolicitudEstado.RESERVADA) {
                        hab.setEstado(HabitacionEstado.RESERVADO);
                        habitacionRepository.save(hab);
                    } else if (updated.getEstado() == SolicitudEstado.ACEPTADA) {
                        hab.setEstado(HabitacionEstado.OCUPADO);
                        habitacionRepository.save(hab);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[SOLICITUD] Error al actualizar estado de habitación: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Enviar notificación al solicitante si cambió el estado
        try {
            if (estadoAnterior != null && !estadoAnterior.equals(updated.getEstado()) && updated.getEstudiante() != null) {
                String titulo = "";
                String mensaje = "";
                String tipo = "";
                
                // Cargar residencia completa para obtener el nombre
                String nombreResidencia = "la residencia";
                if (updated.getResidencia() != null && updated.getResidencia().getId() != null) {
                    Optional<Residencia> residenciaOpt = residenciaRepository.findById(updated.getResidencia().getId());
                    if (residenciaOpt.isPresent()) {
                        nombreResidencia = residenciaOpt.get().getNombre();
                    }
                }
                
                if (updated.getEstado() == SolicitudEstado.ACEPTADA) {
                    titulo = "¡Solicitud aceptada!";
                    mensaje = "Tu solicitud para " + nombreResidencia + " ha sido aceptada. Pronto recibirás el contrato.";
                    tipo = "SOLICITUD_ACEPTADA";
                    System.out.println("[NOTIFICACION] Enviando SOLICITUD_ACEPTADA al estudiante ID: " + updated.getEstudiante().getId());
                } else if (updated.getEstado() == SolicitudEstado.RECHAZADA) {
                    titulo = "Solicitud rechazada";
                    mensaje = "Tu solicitud para " + nombreResidencia + " ha sido rechazada. Puedes buscar otras opciones disponibles.";
                    tipo = "SOLICITUD_RECHAZADA";
                    System.out.println("[NOTIFICACION] Enviando SOLICITUD_RECHAZADA al estudiante ID: " + updated.getEstudiante().getId());
                }
                
                if (!tipo.isEmpty()) {
                    notificationService.createNotification(
                        updated.getEstudiante(),
                        tipo,
                        titulo,
                        mensaje,
                        "SolicitudAlojamiento",
                        updated.getId()
                    );
                    System.out.println("[NOTIFICACION] Notificación de cambio de estado enviada exitosamente");
                }
            }
        } catch (Exception e) {
            System.err.println("[NOTIFICACION] Error al enviar notificación de cambio de estado: " + e.getMessage());
            e.printStackTrace();
        }
        
        return updated;
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

    @Override
    public Optional<SolicitudAlojamiento> findByHabitacionIdAndEstudianteId(Long habitacionId, Long estudianteId) {
        // Only consider solicitudes that are PENDIENTE: if it's not pending, treat as not existing for cancellation
        return repository.findByHabitacionIdAndEstudianteIdAndEstado(habitacionId, estudianteId, SolicitudEstado.PENDIENTE);
    }

    @Override
    public List<SolicitudAlojamiento> findByHabitacionIdOrderByFechaSolicitudDesc(Long habitacionId) {
        return repository.findByHabitacionIdOrderByFechaSolicitudDesc(habitacionId);
    }
}
