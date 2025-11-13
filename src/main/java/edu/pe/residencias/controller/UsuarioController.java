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
import edu.pe.residencias.model.dto.UsuarioCreateDTO;
import edu.pe.residencias.model.entity.Usuario;
import edu.pe.residencias.service.UsuarioService;
import edu.pe.residencias.security.JwtUtil;
import edu.pe.residencias.controller.auth.ErrorResponse;
import io.jsonwebtoken.Claims;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    
    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<Usuario>> readAll() {
        try {
            List<Usuario> usuarios = usuarioService.readAll();
            if (usuarios.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(usuarios, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<Usuario> crear(@Valid @RequestBody UsuarioCreateDTO usuarioDto) {
        try {
            // Log incoming registration data (mask password length)
            String pwd = usuarioDto.getPassword() == null ? "" : usuarioDto.getPassword();
            String pwdMask = pwd.isEmpty() ? "" : "*".repeat(Math.min(pwd.length(), 4)) + "(" + pwd.length() + ")";
            System.out.println("[UsuarioController] Registro attempt - username='" + usuarioDto.getUsername() + "', email='" + usuarioDto.getEmail() + "', password='" + pwdMask + "', nombre='" + usuarioDto.getNombre() + "', apellido='" + usuarioDto.getApellido() + "', dni='" + usuarioDto.getDni() + "'");

            Usuario u = usuarioService.createFromDTO(usuarioDto);
            System.out.println("[UsuarioController] Registro success - usuarioId=" + (u.getId() == null ? "null" : u.getId()) + ", uuid=" + u.getUuid());
            return new ResponseEntity<>(u, HttpStatus.CREATED);
        } catch (Exception e) {
            // Print stacktrace to console so you can see the error cause
            System.err.println("[UsuarioController] Registro failed: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || authHeader.isEmpty()) {
                return new ResponseEntity<>(new ErrorResponse("No token provided"), HttpStatus.UNAUTHORIZED);
            }
            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
            Claims claims = jwtUtil.parseToken(token);
            String uuid = claims.get("uid", String.class);
            if (uuid == null) {
                return new ResponseEntity<>(new ErrorResponse("Invalid token"), HttpStatus.UNAUTHORIZED);
            }
            var opt = usuarioService.findByUuid(uuid);
            if (opt.isEmpty()) {
                return new ResponseEntity<>(new ErrorResponse("User not found"), HttpStatus.NOT_FOUND);
            }
            Usuario u = opt.get();
            // Prepare a minimal profile map for frontend
            java.util.Map<String, Object> profile = new java.util.HashMap<>();
            profile.put("id", u.getId());
            profile.put("username", u.getUsername());
            profile.put("email", u.getPersona() == null ? null : u.getPersona().getEmail());
            if (u.getRol() != null) {
                profile.put("rol", u.getRol().getNombre());
                profile.put("rol_id", u.getRol().getId());
            }
            // include uuid as well
            profile.put("uuid", u.getUuid());
            return new ResponseEntity<>(profile, HttpStatus.OK);
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            return new ResponseEntity<>(new ErrorResponse("Token expired"), HttpStatus.UNAUTHORIZED);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(new ErrorResponse("Failed to parse token"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> getUsuarioId(@PathVariable("id") Long id) {
        try {
            Optional<Usuario> opt = usuarioService.read(id);
            if (opt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            Usuario u = opt.get();
            return new ResponseEntity<>(u, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Usuario> delUsuario(@PathVariable("id") Long id) {
        try {
            usuarioService.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUsuario(@PathVariable("id") Long id, @Valid @RequestBody Usuario usuario) {
        Optional<Usuario> u = usuarioService.read(id);
        if (u.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            Usuario updatedUsuario = usuarioService.update(usuario);
            return new ResponseEntity<>(updatedUsuario, HttpStatus.OK);
        }
    }
}
