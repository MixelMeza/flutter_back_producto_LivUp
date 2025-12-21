package edu.pe.residencias.controller.feedback;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import io.jsonwebtoken.Claims;

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
    public ResponseEntity<?> create(HttpServletRequest request, @RequestBody Map<String, String> body) {
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
            // Require Authorization header and associate the user from the JWT
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return new ResponseEntity<>(Map.of("error", "Token de autorización requerido"), HttpStatus.UNAUTHORIZED);
            }
            try {
                String token = authHeader.substring(7).trim();
                Claims claims = jwtUtil.parseToken(token);
                String username = claims.get("user", String.class);
                var uOpt = usuarioService.findByUsernameOrEmail(username);
                if (uOpt.isPresent()) {
                    f.setUsuario(uOpt.get());
                } else {
                    return new ResponseEntity<>(Map.of("error", "Usuario no encontrado"), HttpStatus.NOT_FOUND);
                }
            } catch (Exception ex) {
                return new ResponseEntity<>(Map.of("error", "Token inválido o expirado"), HttpStatus.UNAUTHORIZED);
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
