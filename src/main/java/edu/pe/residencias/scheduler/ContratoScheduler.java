package edu.pe.residencias.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import edu.pe.residencias.model.entity.Contrato;
import edu.pe.residencias.model.enums.ContratoEstado;
import edu.pe.residencias.repository.ContratoRepository;
import edu.pe.residencias.repository.ResidenciaRepository;
import edu.pe.residencias.service.NotificationService;

@Component
public class ContratoScheduler {

    @Autowired
    private ContratoRepository contratoRepository;
    
    @Autowired
    private ResidenciaRepository residenciaRepository;
    
    @Autowired
    private NotificationService notificationService;

    /**
     * Job que se ejecuta todos los días a las 8:00 AM
     * Verifica contratos que vencen en 30 días y envía notificaciones
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void verificarContratosProximosAVencer() {
        System.out.println("[SCHEDULER] Iniciando verificación de contratos próximos a vencer...");
        
        try {
            LocalDate fechaLimite = LocalDate.now().plusDays(30);
            List<Contrato> contratos = contratoRepository.findAll();
            
            int notificacionesEnviadas = 0;
            
            for (Contrato contrato : contratos) {
                // Solo verificar contratos vigentes
                if (contrato.getEstado() == ContratoEstado.VIGENTE && 
                    contrato.getFechaFin() != null &&
                    contrato.getFechaFin().isEqual(fechaLimite)) {
                    
                    // Obtener información necesaria
                    if (contrato.getSolicitud() == null) continue;
                    
                    var estudiante = contrato.getSolicitud().getEstudiante();
                    var residenciaId = contrato.getSolicitud().getResidencia() != null ? 
                                      contrato.getSolicitud().getResidencia().getId() : null;
                    
                    if (estudiante == null || residenciaId == null) continue;
                    
                    // Cargar propietario de la residencia
                    var residenciaOpt = residenciaRepository.findById(residenciaId);
                    if (!residenciaOpt.isPresent()) continue;
                    
                    var residencia = residenciaOpt.get();
                    var propietario = residencia.getUsuario();
                    
                    if (propietario == null) continue;
                    
                    String nombreResidencia = residencia.getNombre();
                    String fechaVencimiento = contrato.getFechaFin().toString();
                    
                    // 🔔 Notificar al ESTUDIANTE
                    try {
                        String tituloEstudiante = "⚠️ Contrato próximo a vencer";
                        String mensajeEstudiante = "Tu contrato en " + nombreResidencia + 
                            " vence el " + fechaVencimiento + " (en 30 días). Contacta al propietario si deseas renovar.";
                        
                        notificationService.createNotification(
                            estudiante,
                            "CONTRATO_VENCIMIENTO_PROXIMO",
                            tituloEstudiante,
                            mensajeEstudiante,
                            "Contrato",
                            contrato.getId()
                        );
                        
                        System.out.println("[SCHEDULER] Notificación enviada al estudiante ID: " + estudiante.getId());
                        notificacionesEnviadas++;
                    } catch (Exception e) {
                        System.err.println("[SCHEDULER] Error al notificar estudiante: " + e.getMessage());
                    }
                    
                    // 🔔 Notificar al PROPIETARIO
                    try {
                        String tituloPropietario = "⚠️ Contrato próximo a vencer";
                        String mensajePropietario = "El contrato de un inquilino en " + nombreResidencia + 
                            " vence el " + fechaVencimiento + " (en 30 días). Considera contactarlo para renovación.";
                        
                        notificationService.createNotification(
                            propietario,
                            "CONTRATO_VENCIMIENTO_PROXIMO",
                            tituloPropietario,
                            mensajePropietario,
                            "Contrato",
                            contrato.getId()
                        );
                        
                        System.out.println("[SCHEDULER] Notificación enviada al propietario ID: " + propietario.getId());
                        notificacionesEnviadas++;
                    } catch (Exception e) {
                        System.err.println("[SCHEDULER] Error al notificar propietario: " + e.getMessage());
                    }
                }
            }
            
            System.out.println("[SCHEDULER] Verificación completada. Notificaciones enviadas: " + notificacionesEnviadas);
            
        } catch (Exception e) {
            System.err.println("[SCHEDULER] Error en verificación de contratos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Método para ejecutar manualmente (útil para testing)
     * Puede ser llamado desde un endpoint REST si necesitas probarlo
     */
    public void ejecutarManualmente() {
        System.out.println("[SCHEDULER] Ejecución manual iniciada...");
        verificarContratosProximosAVencer();
    }
}
