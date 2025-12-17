package edu.pe.residencias.controller;

import java.util.List;
import java.util.Optional;

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
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import edu.pe.residencias.model.entity.Ubicacion;
import edu.pe.residencias.service.UbicacionService;
import edu.pe.residencias.security.JwtUtil;
import edu.pe.residencias.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/ubicaciones")
public class UbicacionController {

    @Autowired
    private UbicacionService ubicacionService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<List<Ubicacion>> readAll(HttpServletRequest request) {
        try {
            // require authenticated user with verified email
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            var claims = jwtUtil.parseToken(token);
            String uid = claims.get("uid", String.class);
            if (uid == null || uid.isBlank()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            var usuarioOpt = usuarioRepository.findByUuid(uid);
            if (usuarioOpt.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            var usuario = usuarioOpt.get();
            if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }

            List<Ubicacion> ubicaciones = ubicacionService.readAll();
            if (ubicaciones.isEmpty()) return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            return new ResponseEntity<>(ubicaciones, HttpStatus.OK);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<Ubicacion> crear(HttpServletRequest request, @Valid @RequestBody Ubicacion ubicacion) {
        try {
            // require authenticated verified user
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            var claims = jwtUtil.parseToken(token);
            String uid = claims.get("uid", String.class);
            if (uid == null || uid.isBlank()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            var usuarioOpt = usuarioRepository.findByUuid(uid);
            if (usuarioOpt.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            var usuario = usuarioOpt.get();
            if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Ubicacion u = ubicacionService.create(ubicacion);
            return new ResponseEntity<>(u, HttpStatus.CREATED);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ubicacion> getUbicacionId(@PathVariable("id") Long id) {
        try {
            Ubicacion u = ubicacionService.read(id).get();
            return new ResponseEntity<>(u, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Ubicacion> delUbicacion(@PathVariable("id") Long id) {
        try {
            ubicacionService.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUbicacion(@PathVariable("id") Long id, @Valid @RequestBody Ubicacion ubicacion) {
        Optional<Ubicacion> u = ubicacionService.read(id);
        if (u.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            // ensure the incoming object refers to the existing id to avoid creating a new row
            ubicacion.setId(id);
            // alternatively, copy only provided fields into existing entity to avoid nulling fields
            // Ubicacion existing = u.get();
            // if (ubicacion.getDireccion() != null) existing.setDireccion(ubicacion.getDireccion());
            // ... (copy other fields similarly)
            Ubicacion updatedUbicacion = ubicacionService.update(ubicacion);
            return new ResponseEntity<>(updatedUbicacion, HttpStatus.OK);
        }
    }
}
