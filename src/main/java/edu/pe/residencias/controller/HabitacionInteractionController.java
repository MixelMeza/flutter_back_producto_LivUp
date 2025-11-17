package edu.pe.residencias.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.pe.residencias.model.entity.VistaReciente;
import edu.pe.residencias.security.JwtUtil;
import edu.pe.residencias.service.FavoritoService;
import edu.pe.residencias.service.VistaRecienteService;
import edu.pe.residencias.repository.UsuarioRepository;
import edu.pe.residencias.model.entity.Usuario;

@RestController
@RequestMapping("/api/habitaciones")
public class HabitacionInteractionController {

    @Autowired
    private FavoritoService favoritoService;

    @Autowired
    private VistaRecienteService vistaService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // POST /api/habitaciones/{id}/like  -> mark like
    @PostMapping("/{id}/like")
    public ResponseEntity<?> like(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = extractUserIdFromAuth(authHeader);
        if (userId == null) return ResponseEntity.status(401).build();
        favoritoService.like(userId, id);
        return ResponseEntity.ok().build();
    }

    // DELETE /api/habitaciones/{id}/like -> unlike
    @DeleteMapping("/{id}/like")
    public ResponseEntity<?> unlike(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = extractUserIdFromAuth(authHeader);
        if (userId == null) return ResponseEntity.status(401).build();
        favoritoService.unlike(userId, id);
        return ResponseEntity.ok().build();
    }

    // GET likes info
    @GetMapping("/{id}/likes")
    public ResponseEntity<?> likesInfo(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = extractUserIdFromAuth(authHeader);
        long count = favoritoService.countLikes(id);
        boolean liked = userId != null && favoritoService.isLiked(userId, id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("count", count);
        resp.put("likedByMe", liked);
        return ResponseEntity.ok(resp);
    }

    // POST to record view. If user not authenticated, accept sessionUuid in body or query param
    @PostMapping("/{id}/view")
    public ResponseEntity<?> recordView(@PathVariable Long id,
                                        @RequestHeader(value = "Authorization", required = false) String authHeader,
                                        @RequestParam(value = "sessionUuid", required = false) String sessionUuid) {
        Long userId = extractUserIdFromAuth(authHeader);
        if (userId != null) {
            vistaService.recordViewForUser(userId, id);
            return ResponseEntity.ok().build();
        }
        if (sessionUuid != null && !sessionUuid.isEmpty()) {
            vistaService.recordViewForSession(sessionUuid, id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("No user or sessionUuid provided");
    }

    // GET recent views for authenticated user
    @GetMapping("/recent")
    public ResponseEntity<?> recentForMe(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                         @RequestParam(value = "limit", required = false, defaultValue = "20") int limit) {
        Long userId = extractUserIdFromAuth(authHeader);
        if (userId == null) return ResponseEntity.status(401).build();
        List<VistaReciente> list = vistaService.getRecentForUser(userId, limit);
        return ResponseEntity.ok(list);
    }

    // GET recent for session (anonymous)
    @GetMapping("/recent/session")
    public ResponseEntity<?> recentForSession(@RequestParam(value = "sessionUuid") String sessionUuid,
                                              @RequestParam(value = "limit", required = false, defaultValue = "20") int limit) {
        if (sessionUuid == null || sessionUuid.isEmpty()) return ResponseEntity.badRequest().build();
        List<VistaReciente> list = vistaService.getRecentForSession(sessionUuid, limit);
        return ResponseEntity.ok(list);
    }

    private Long extractUserIdFromAuth(String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) return null;
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        try {
            io.jsonwebtoken.Claims claims = jwtUtil.parseToken(token);
            String uuid = claims.get("uid", String.class);
            if (uuid == null) return null;
            java.util.Optional<Usuario> opt = usuarioRepository.findByUuid(uuid);
            return opt.map(Usuario::getId).orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }
}
