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
import edu.pe.residencias.model.entity.Residencia;
import edu.pe.residencias.model.enums.ResidenciaEstado;
import edu.pe.residencias.service.ResidenciaService;
import edu.pe.residencias.util.ServiciosUtil;
import edu.pe.residencias.repository.UbicacionRepository;
import edu.pe.residencias.repository.UniversidadRepository;
import edu.pe.residencias.repository.UsuarioRepository;
import edu.pe.residencias.security.JwtUtil;

@RestController
@RequestMapping("/api/residencias")
public class ResidenciaController {
    
    @Autowired
    private ResidenciaService residenciaService;

    @Autowired
    private UbicacionRepository ubicacionRepository;

    @Autowired
    private UniversidadRepository universidadRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<Residencia>> readAll() {
        try {
            List<Residencia> residencias = residenciaService.readAll();
            if (residencias.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(residencias, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<Residencia> crear(@Valid @RequestBody Residencia residencia) {
        try {
            // Validate and set estado default
            if (residencia.getEstado() == null || residencia.getEstado().isBlank()) {
                residencia.setEstado(ResidenciaEstado.ACTIVO.toString());
            } else if (!ResidenciaEstado.isValid(residencia.getEstado())) {
                return ResponseEntity.badRequest().body(null);
            }

            // Normalize servicios field (simple comma-separated words)
            residencia.setServicios(ServiciosUtil.normalizeServiciosText(residencia.getServicios()));
            // If ubicacion is provided as nested object, persist it first
            if (residencia.getUbicacion() != null) {
                var ub = residencia.getUbicacion();
                // Save ubicacion (will set id)
                ubicacionRepository.save(ub);
                residencia.setUbicacion(ub);
            }

            // Assign universidad_id = 1 if not provided
            if (residencia.getUniversidad() == null) {
                universidadRepository.findById(1L).ifPresent(residencia::setUniversidad);
            }

            // Assign usuario from token if provided in Authorization header
            // Note: controller-level methods don't have auth header param here; try to obtain from SecurityContext
            try {
                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                    // nothing - we prefer to resolve by token if possible
                }
            } catch (Exception ex) {
                // ignore
            }

            Residencia r = residenciaService.create(residencia);
            return new ResponseEntity<>(r, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Residencia> getResidenciaId(@PathVariable("id") Long id) {
        try {
            Residencia r = residenciaService.read(id).get();
            return new ResponseEntity<>(r, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Residencia> delResidencia(@PathVariable("id") Long id) {
        try {
            residenciaService.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateResidencia(@PathVariable("id") Long id, @Valid @RequestBody Residencia residencia) {
        Optional<Residencia> r = residenciaService.read(id);
        if (r.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            // Validate estado if provided
            if (residencia.getEstado() != null && !residencia.getEstado().isBlank()) {
                if (!ResidenciaEstado.isValid(residencia.getEstado())) {
                    return new ResponseEntity<>("Invalid estado value", HttpStatus.BAD_REQUEST);
                }
            }
            residencia.setServicios(ServiciosUtil.normalizeServiciosText(residencia.getServicios()));
            Residencia updatedResidencia = residenciaService.update(residencia);
            return new ResponseEntity<>(updatedResidencia, HttpStatus.OK);
        }
    }
}
