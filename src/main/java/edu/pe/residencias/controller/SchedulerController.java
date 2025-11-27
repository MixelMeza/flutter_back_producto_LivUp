package edu.pe.residencias.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pe.residencias.scheduler.ContratoScheduler;

/**
 * Controlador para ejecutar tareas programadas manualmente
 * Útil para testing y debugging
 */
@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

    @Autowired
    private ContratoScheduler contratoScheduler;

    /**
     * Ejecutar verificación de contratos próximos a vencer manualmente
     * POST http://localhost:8080/api/scheduler/verificar-contratos
     */
    @PostMapping("/verificar-contratos")
    public ResponseEntity<?> verificarContratos() {
        try {
            contratoScheduler.ejecutarManualmente();
            return ResponseEntity.ok()
                .body(new SuccessResponse("Verificación de contratos ejecutada exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Error al ejecutar verificación: " + e.getMessage()));
        }
    }
    
    // DTOs internos para respuestas
    record SuccessResponse(String message) {}
    record ErrorResponse(String error) {}
}
