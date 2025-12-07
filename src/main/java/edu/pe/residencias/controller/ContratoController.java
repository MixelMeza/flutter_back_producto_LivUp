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
import jakarta.servlet.http.HttpServletRequest;
import edu.pe.residencias.model.entity.Contrato;
import edu.pe.residencias.model.dto.ContratoResumidoDTO;
import edu.pe.residencias.service.ContratoService;
import edu.pe.residencias.security.JwtUtil;
import edu.pe.residencias.repository.ResidenciaRepository;
import edu.pe.residencias.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/contratos")
public class ContratoController {
    // Endpoint para obtener el historial de contratos de un inquilino (excepto
    // vigente)
    @GetMapping("/historial/usuario/{usuarioId}")
    public ResponseEntity<?> getHistorialContratosByUsuarioId(@PathVariable("usuarioId") Long usuarioId) {
        List<Contrato> historial = contratoService.getHistorialContratosByUsuarioId(usuarioId);
        if (historial.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No se encontraron contratos previos para este usuario");
        }
        return ResponseEntity.ok(historial);
    }

    @Autowired
    private ContratoService contratoService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ResidenciaRepository residenciaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<List<Contrato>> readAll() {
        try {
            List<Contrato> contratos = contratoService.readAll();
            if (contratos.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(contratos, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<Contrato> crear(@Valid @RequestBody Contrato contrato) {
        try {
            Contrato c = contratoService.create(contrato);
            return new ResponseEntity<>(c, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace(); // Mostrar el error en la consola
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contrato> getContratoId(@PathVariable("id") Long id) {
        try {
            Contrato c = contratoService.read(id).get();
            return new ResponseEntity<>(c, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Contrato> delContrato(@PathVariable("id") Long id) {
        try {
            contratoService.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateContrato(@PathVariable("id") Long id, @Valid @RequestBody Contrato contrato) {
        Optional<Contrato> c = contratoService.read(id);
        if (c.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            Contrato updatedContrato = contratoService.update(contrato);
            return new ResponseEntity<>(updatedContrato, HttpStatus.OK);
        }
    }

    // NUEVO: Contratos vigentes del propietario
    @GetMapping("/propietario/{residenciaId}/vigentes")
    public ResponseEntity<?> getContratosVigorosPropietario(
            HttpServletRequest request,
            @PathVariable("residenciaId") Long residenciaId) {
        try {
            // Obtener token y validar
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Falta Authorization header");
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            var claims = jwtUtil.parseToken(token);
            String uid = claims.get("uid", String.class);
            if (uid == null || uid.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido: uid faltante");
            }

            // Validar que la residencia existe y pertenece al propietario
            var residenciaOpt = residenciaRepository.findById(residenciaId);
            if (residenciaOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Residencia no encontrada");
            }

            var residencia = residenciaOpt.get();
            var usuarioOpt = usuarioRepository.findByUuid(uid);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
            }

            var usuario = usuarioOpt.get();
            if (!isOwnerOrAdmin(usuario, residencia)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No tienes permiso para ver estos contratos");
            }

            // Obtener contratos vigentes
            List<Contrato> contratos = contratoService.findVigorosByResidenciaId(residenciaId);
            if (contratos.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            // Mapear a DTOs
            List<ContratoResumidoDTO> dtos = contratoService.mapToContratoResumidoDTOs(contratos);
            return new ResponseEntity<>(dtos, HttpStatus.OK);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isOwnerOrAdmin(edu.pe.residencias.model.entity.Usuario usuario, edu.pe.residencias.model.entity.Residencia residencia) {
        if (usuario == null) return false;
        try {
            if (usuario.getRol() != null && "admin".equalsIgnoreCase(usuario.getRol().getNombre())) return true;
        } catch (Exception ignore) {}
        if (residencia == null) return false;
        if (residencia.getUsuario() != null && residencia.getUsuario().getId() != null && residencia.getUsuario().getId().equals(usuario.getId())) return true;
        return false;
    }

    // Endpoint para obtener el contrato vigente de un inquilino por su id
    @GetMapping("/vigente/usuario/{usuarioId}")
    public ResponseEntity<?> getContratoVigenteByUsuarioId(@PathVariable("usuarioId") Long usuarioId) {
        Optional<Contrato> contrato = contratoService.getContratoVigenteByUsuarioId(usuarioId);
        if (contrato.isPresent()) {
            return new ResponseEntity<>(contrato.get(), HttpStatus.OK);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No cuenta con contrato vigente");
        }
    }
}
