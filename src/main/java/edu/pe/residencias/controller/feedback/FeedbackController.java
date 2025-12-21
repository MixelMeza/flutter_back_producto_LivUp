package edu.pe.residencias.controller.feedback;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.pe.residencias.model.entity.Feedback;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    @Autowired
    private edu.pe.residencias.service.FeedbackService feedbackService;
    @Autowired
    private edu.pe.residencias.service.UsuarioService usuarioService;
    @Autowired
    private edu.pe.residencias.security.JwtUtil jwtUtil;

    // Public: create feedback
    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        try {
            String tipo = body.get("tipo");
            String titulo = body.get("titulo");
            String mensaje = body.get("mensaje");
            if (tipo == null || mensaje == null || mensaje.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Los campos 'tipo' y 'mensaje' son requeridos"));
            }

            Feedback.Tipo t;
            try { t = Feedback.Tipo.valueOf(tipo); } catch (Exception ex) { t = Feedback.Tipo.otro; }

            Feedback f = new Feedback();
            f.setTipo(t);
            f.setTitulo(titulo == null ? "" : titulo);
            f.setMensaje(mensaje);
            f.setEstado(Feedback.Estado.pendiente);
            // Si el cliente está autenticado, obtener el usuario desde el JWT (no confiar en userId del body)
            try {
                String authHeader = null;
                // header may be provided in request map under "Authorization" or client should set header; prefer header
                // We'll not rely on body.userId; check request headers by accessing a ThreadLocal via RequestContextHolder is overkill here,
                // so attempt to read Authorization from system properties not feasible. Instead, expect client to set header; try to read via JwtUtil if provided later.
            } catch (Exception ignore) {
                // fallback: anonymous
            }
            // NOTE: the controller method cannot access HttpServletRequest here because create() signature currently lacks it.
            // We'll attempt to read token if present via a standard approach: check SecurityContext (if Spring Security is configured).
            try {
                // If JwtUtil and UsuarioService are available, try to read authenticated principal from SecurityContext
                var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof String) {
                    String username = (String) authentication.getPrincipal();
                    var uOpt = usuarioService.findByUsernameOrEmail(username);
                    if (uOpt.isPresent()) f.setUsuario(uOpt.get());
                }
            } catch (NoClassDefFoundError | Exception ex) {
                // If SecurityContext is not configured or other error, ignore and leave feedback anonymous
            }
            Feedback saved = feedbackService.create(f);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error interno"));
        }
    }

    // Admin check moved to FeedbackService

    // Admin: list feedbacks (optional filters)
    @GetMapping("")
    public ResponseEntity<?> list(HttpServletRequest request,
                                  @RequestParam(value = "tipo", required = false) String tipo,
                                  @RequestParam(value = "estado", required = false) String estado) {
        var admin = feedbackService.getAdminFromRequest(request);
        if (admin.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo administradores"));

        try {
            if (tipo != null) {
                try { return ResponseEntity.ok(feedbackService.findByTipo(Feedback.Tipo.valueOf(tipo))); } catch (Exception ex) { /* ignore */ }
            }
            if (estado != null) {
                try { return ResponseEntity.ok(feedbackService.findByEstado(Feedback.Estado.valueOf(estado))); } catch (Exception ex) { /* ignore */ }
            }
            return ResponseEntity.ok(feedbackService.listAll());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error interno"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(HttpServletRequest request, @PathVariable Long id) {
        var admin = feedbackService.getAdminFromRequest(request);
        if (admin.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo administradores"));
        Optional<Feedback> opt = feedbackService.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No encontrado"));
        }
        return ResponseEntity.ok(opt.get());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, String> body) {
        var admin = feedbackService.getAdminFromRequest(request);
        if (admin.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo administradores"));

        Optional<Feedback> opt = feedbackService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No encontrado"));
        Feedback f = opt.get();
        String tipo = body.get("tipo");
        String titulo = body.get("titulo");
        String mensaje = body.get("mensaje");
        String estado = body.get("estado");
        if (tipo != null) {
            try { f.setTipo(Feedback.Tipo.valueOf(tipo)); } catch (Exception ex) { }
        }
        if (titulo != null) f.setTitulo(titulo);
        if (mensaje != null) f.setMensaje(mensaje);
        if (estado != null) {
            try { f.setEstado(Feedback.Estado.valueOf(estado)); } catch (Exception ex) { }
        }
        feedbackService.save(f);
        return ResponseEntity.ok(f);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(HttpServletRequest request, @PathVariable Long id) {
        var admin = feedbackService.getAdminFromRequest(request);
        if (admin.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo administradores"));
        Optional<Feedback> opt = feedbackService.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No encontrado"));
        feedbackService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Eliminado"));
    }
}
