package edu.pe.residencias.controller;

import edu.pe.residencias.model.entity.VistaReciente;
import edu.pe.residencias.service.VistaRecienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vistas-recientes")
public class VistaRecienteController {

    @Autowired
    private VistaRecienteService vistaRecienteService;

    // POST para usuario autenticado
    @PostMapping
    public void recordViewForUser(@RequestBody VistaReciente body) {
        vistaRecienteService.recordViewForUser(body.getUsuario().getId(), body.getHabitacion().getId());
    }

    // GET todas las vistas recientes (admin)
    @GetMapping
    public List<VistaReciente> listarTodas() {
        // ...existing code...
        throw new UnsupportedOperationException("Implementar si es necesario");
    }

    // GET por usuario
    @GetMapping("/user/{usuarioId}")
    public List<VistaReciente> listarPorUsuario(@PathVariable Long usuarioId, @RequestParam(defaultValue = "10") int limit) {
        return vistaRecienteService.getRecentForUser(usuarioId, limit);
    }

    // GET por sesión
    @GetMapping("/session/{sessionUuid}")
    public List<VistaReciente> listarPorSession(@PathVariable String sessionUuid, @RequestParam(defaultValue = "10") int limit) {
        return vistaRecienteService.getRecentForSession(sessionUuid, limit);
    }

    // DELETE por usuario
    @DeleteMapping("/user/{usuarioId}")
    public void eliminarPorUsuario(@PathVariable Long usuarioId) {
        vistaRecienteService.clearRecentForUser(usuarioId);
    }
}
