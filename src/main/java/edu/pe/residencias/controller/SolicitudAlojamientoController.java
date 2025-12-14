package edu.pe.residencias.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import edu.pe.residencias.model.entity.SolicitudAlojamiento;
import edu.pe.residencias.service.SolicitudAlojamientoService;
import edu.pe.residencias.model.enums.SolicitudEstado;

@RestController
@RequestMapping("/api/solicitudes-alojamiento")
public class SolicitudAlojamientoController {
    private static final Logger logger = LoggerFactory.getLogger(SolicitudAlojamientoController.class);
    
    @Autowired
    private SolicitudAlojamientoService solicitudAlojamientoService;

    @GetMapping
    public ResponseEntity<List<SolicitudAlojamiento>> readAll() {
        try {
            List<SolicitudAlojamiento> solicitudes = solicitudAlojamientoService.readAll();
            if (solicitudes.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(solicitudes, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody SolicitudAlojamiento solicitudAlojamiento) {
        try {
            SolicitudAlojamiento s = solicitudAlojamientoService.create(solicitudAlojamiento);
            return new ResponseEntity<>(s, HttpStatus.CREATED);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("error", ex.getMessage()));
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitudAlojamiento> getSolicitudAlojamientoId(@PathVariable("id") Long id) {
        try {
            SolicitudAlojamiento s = solicitudAlojamientoService.read(id).get();
            return new ResponseEntity<>(s, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SolicitudAlojamiento> delSolicitudAlojamiento(@PathVariable("id") Long id) {
        try {
            solicitudAlojamientoService.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSolicitudAlojamiento(@PathVariable("id") Long id, @Valid @RequestBody SolicitudAlojamiento solicitudAlojamiento) {
        Optional<SolicitudAlojamiento> s = solicitudAlojamientoService.read(id);
        if (s.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            SolicitudAlojamiento updatedSolicitudAlojamiento = solicitudAlojamientoService.update(solicitudAlojamiento);
            return new ResponseEntity<>(updatedSolicitudAlojamiento, HttpStatus.OK);
        }
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelarSolicitud(@PathVariable("id") Long id) {
        Optional<SolicitudAlojamiento> s = solicitudAlojamientoService.read(id);
        if (s.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        try {
            SolicitudAlojamiento solicitud = s.get();
            solicitud.setEstado(SolicitudEstado.CANCELADA);
            SolicitudAlojamiento updated = solicitudAlojamientoService.update(solicitud);
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/habitacion/{habitacionId}/estudiante/{estudianteId}")
    public ResponseEntity<?> obtenerSolicitudPorHabitacionYEstudiante(@PathVariable("habitacionId") Long habitacionId,
                                                                       @PathVariable("estudianteId") Long estudianteId) {
        try {
            var opt = solicitudAlojamientoService.findByHabitacionIdAndEstudianteId(habitacionId, estudianteId);
            if (opt.isPresent()) {
                return new ResponseEntity<>(java.util.Map.of("id", opt.get().getId()), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}/info")
    public ResponseEntity<?> obtenerInfoSolicitudPorId(@PathVariable("id") Long id) {
        try {
            var opt = solicitudAlojamientoService.read(id);
            if (opt.isPresent()) {
                var s = opt.get();
                java.util.Map<String, Object> resp = new java.util.HashMap<>();
                resp.put("id", s.getId());
                resp.put("fijo", s.getFijo());
                resp.put("fechaInicio", s.getFechaInicio());
                // allow null fechaFin
                resp.put("fechaFin", s.getFechaFin());
                return ResponseEntity.ok(resp);
            } else {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        } catch (Exception e) {
            logger.error("Error en obtenerInfoSolicitudPorId id={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/habitacion/{habitacionId}/estudiante/{estudianteId}/info")
    public ResponseEntity<?> obtenerInfoSolicitudPorHabitacionYEstudiante(@PathVariable("habitacionId") Long habitacionId,
                                                                           @PathVariable("estudianteId") Long estudianteId) {
        try {
            var opt = solicitudAlojamientoService.findByHabitacionIdAndEstudianteId(habitacionId, estudianteId);
            if (opt.isPresent()) {
                var s = opt.get();
                java.util.Map<String, Object> resp = new java.util.HashMap<>();
                resp.put("id", s.getId());
                resp.put("fijo", s.getFijo());
                resp.put("fechaInicio", s.getFechaInicio());
                resp.put("fechaFin", s.getFechaFin());
                resp.put("descripcion", s.getComentarios());
                return ResponseEntity.ok(resp);
            } else {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        } catch (Exception e) {
            logger.error("Error en obtenerInfoSolicitudPorHabitacionYEstudiante habId={} estudianteId={}", habitacionId, estudianteId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/habitacion/{habitacionId}/solicitudes")
    public ResponseEntity<?> listarSolicitudesPorHabitacion(@PathVariable("habitacionId") Long habitacionId) {
        try {
            var list = solicitudAlojamientoService.findByHabitacionIdOrderByFechaSolicitudDesc(habitacionId);
            if (list == null || list.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            java.util.List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
            for (var s : list) {
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("id", s.getId());
                m.put("fechaSolicitud", s.getFechaSolicitud());
                m.put("fechaInicio", s.getFechaInicio());
                m.put("fechaFin", s.getFechaFin());
                m.put("duracionMeses", s.getDuracionMeses());
                m.put("comentarios", s.getComentarios());
                m.put("estado", s.getEstado());
                if (s.getEstudiante() != null && s.getEstudiante().getPersona() != null) {
                    var p = s.getEstudiante().getPersona();
                    m.put("nombre", (p.getNombre() != null ? p.getNombre() : "") + " " + (p.getApellido() != null ? p.getApellido() : ""));
                    m.put("fotoPerfil", p.getFotoUrl());
                } else {
                    m.put("nombre", null);
                    m.put("fotoPerfil", null);
                }
                out.add(m);
            }
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
