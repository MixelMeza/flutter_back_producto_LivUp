package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.SolicitudAlojamiento;
import edu.pe.residencias.model.entity.Residencia;
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
    private NotificationService notificationService;

    @Override
    public SolicitudAlojamiento create(SolicitudAlojamiento solicitudAlojamiento) {
        if (solicitudAlojamiento.getEstado() == null) {
            solicitudAlojamiento.setEstado(SolicitudEstado.PENDIENTE);
        }
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
}
