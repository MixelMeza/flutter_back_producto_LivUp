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
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import edu.pe.residencias.repository.UsuarioRepository;
import edu.pe.residencias.repository.HabitacionRepository;
import edu.pe.residencias.model.dto.SolicitudResumenDTO;
import io.jsonwebtoken.Claims;
import edu.pe.residencias.util.DateTimeUtil;
import java.time.temporal.ChronoUnit;
import edu.pe.residencias.model.entity.SolicitudAlojamiento;
import edu.pe.residencias.model.dto.SolicitudDetalleDTO;
import edu.pe.residencias.service.SolicitudAlojamientoService;
import edu.pe.residencias.model.enums.SolicitudEstado;

@RestController
@RequestMapping("/api/solicitudes-alojamiento")
public class SolicitudAlojamientoController {
    private static final Logger logger = LoggerFactory.getLogger(SolicitudAlojamientoController.class);
    
    @Autowired
    private SolicitudAlojamientoService solicitudAlojamientoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private HabitacionRepository habitacionRepository;
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
    public ResponseEntity<?> getSolicitudAlojamientoId(@PathVariable("id") Long id) {
        try {
            var opt = solicitudAlojamientoService.read(id);
            if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of("error", "Solicitud no encontrada"));
            SolicitudAlojamiento s = opt.get();

            SolicitudDetalleDTO dto = new SolicitudDetalleDTO();
            dto.setId(s.getId());
            dto.setFechaSolicitud(s.getFechaSolicitud());
            dto.setFechaInicio(s.getFechaInicio());
            dto.setFechaFin(s.getFechaFin());
            dto.setDuracionMeses(s.getDuracionMeses());
            dto.setFijo(s.getFijo());
            dto.setEstado(s.getEstado());
            dto.setComentarios(s.getComentarios());

            if (s.getHabitacion() != null) {
                dto.setHabitacionNombre(s.getHabitacion().getNombre());
                if (s.getHabitacion().getResidencia() != null) dto.setResidenciaNombre(s.getHabitacion().getResidencia().getNombre());
            } else if (s.getResidencia() != null) {
                dto.setResidenciaNombre(s.getResidencia().getNombre());
            }

            if (s.getEstudiante() != null && s.getEstudiante().getPersona() != null) {
                var p = s.getEstudiante().getPersona();
                String nombreCompleto = (p.getNombre() != null ? p.getNombre() : "") + " " + (p.getApellido() != null ? p.getApellido() : "");
                dto.setEstudianteNombreCompleto(nombreCompleto.trim());
                dto.setEstudianteFotoPerfil(p.getFotoUrl());
                dto.setEstudianteEmail(p.getEmail());
                dto.setEstudianteTelefono(p.getTelefono());
                dto.setEstudianteNotas(p.getNotas());
            }

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.error("Error en getSolicitudAlojamientoId id={}", id, e);
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
        try {
            var existing = solicitudAlojamientoService.read(id);
            if (existing.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Solicitud no encontrada");
            }
            solicitudAlojamiento.setId(id);
            SolicitudAlojamiento updatedSolicitudAlojamiento = solicitudAlojamientoService.update(solicitudAlojamiento);
            return new ResponseEntity<>(updatedSolicitudAlojamiento, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", e.getMessage()));
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

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazarSolicitud(@PathVariable("id") Long id, HttpServletRequest req) {
        try {
            Claims claims = (Claims) req.getAttribute("jwtClaims");
            if (claims == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("error", "No JWT claims found"));
            String uid = claims.get("uid", String.class);
            if (uid == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("error", "Invalid token claims"));

            var usuarioOpt = usuarioRepository.findByUuid(uid);
            if (usuarioOpt.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("error", "Usuario no encontrado"));
            var usuario = usuarioOpt.get();

            var opt = solicitudAlojamientoService.read(id);
            if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of("error", "Solicitud no encontrada"));
            var solicitud = opt.get();

            var habitacion = solicitud.getHabitacion();
            if (habitacion == null || habitacion.getResidencia() == null || habitacion.getResidencia().getUsuario() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of("error", "Solicitud sin propietario asociado"));
            }
            var propietario = habitacion.getResidencia().getUsuario();
            boolean isOwner = propietario.getId() != null && propietario.getId().equals(usuario.getId());
            boolean isAdmin = usuario.getRol() != null && "admin".equalsIgnoreCase(usuario.getRol().getNombre());
            if (!isOwner && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of("error", "No eres propietario ni administrador"));
            }

            solicitud.setEstado(SolicitudEstado.RECHAZADA);
            var updated = solicitudAlojamientoService.update(solicitud);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.error("Error al rechazar solicitud id={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", e.getMessage()));
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
    public ResponseEntity<?> listarSolicitudesPorHabitacion(@PathVariable("habitacionId") Long habitacionId, HttpServletRequest req) {
        try {
            // Authorization: only the owner (propietario) of the habitacion's residencia may list solicitudes
            // controllers behind JwtRequestFilter have the parsed claims as request attribute "jwtClaims"
            Claims claims = (Claims) req.getAttribute("jwtClaims");
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("error", "No JWT claims found"));
            }
            String uid = claims.get("uid", String.class);
            if (uid == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("error", "Invalid token claims"));

            var usuarioOpt = usuarioRepository.findByUuid(uid);
            if (usuarioOpt.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("error", "Usuario no encontrado"));
            var usuario = usuarioOpt.get();

            var habOpt = habitacionRepository.findById(habitacionId);
            if (habOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of("error", "Habitación no encontrada"));
            var habitacion = habOpt.get();
            if (habitacion.getResidencia() == null || habitacion.getResidencia().getUsuario() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of("error", "Habitación no tiene propietario asociado"));
            }
            var propietario = habitacion.getResidencia().getUsuario();
            boolean isOwner = propietario.getId().equals(usuario.getId());
            boolean isAdmin = usuario.getRol() != null && "admin".equalsIgnoreCase(usuario.getRol().getNombre());
            if (!isOwner && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Map.of("error", "No eres propietario ni administrador"));
            }

            var list = solicitudAlojamientoService.findByHabitacionIdOrderByFechaSolicitudDesc(habitacionId);
            if (list == null || list.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            // Filter out VENCIDO and CANCELADA
            java.util.List<edu.pe.residencias.model.entity.SolicitudAlojamiento> filtered = new java.util.ArrayList<>();
            for (var s : list) {
                if (s == null || s.getEstado() == null) continue;
                if (s.getEstado() == edu.pe.residencias.model.enums.SolicitudEstado.VENCIDO) continue;
                if (s.getEstado() == edu.pe.residencias.model.enums.SolicitudEstado.CANCELADA) continue;
                filtered.add(s);
            }
            if (filtered.isEmpty()) return new ResponseEntity<>(HttpStatus.NO_CONTENT);

            // Order: PENDIENTE first, then RECHAZADA, then the rest. Within same priority keep fechaSolicitud desc.
            java.util.Map<edu.pe.residencias.model.enums.SolicitudEstado, Integer> priority = new java.util.HashMap<>();
            priority.put(edu.pe.residencias.model.enums.SolicitudEstado.PENDIENTE, 0);
            priority.put(edu.pe.residencias.model.enums.SolicitudEstado.RECHAZADA, 1);

            filtered.sort((a, b) -> {
                int pa = priority.getOrDefault(a.getEstado(), 2);
                int pb = priority.getOrDefault(b.getEstado(), 2);
                if (pa != pb) return Integer.compare(pa, pb);
                java.time.LocalDateTime fa = a.getFechaSolicitud();
                java.time.LocalDateTime fb = b.getFechaSolicitud();
                if (fa == null && fb == null) return 0;
                if (fa == null) return 1;
                if (fb == null) return -1;
                return fb.compareTo(fa); // desc
            });

            java.util.List<SolicitudResumenDTO> out = new java.util.ArrayList<>();
            for (var s : filtered) {
                SolicitudResumenDTO dto = new SolicitudResumenDTO();
                dto.setId(s.getId());
                dto.setFechaSolicitud(s.getFechaSolicitud());
                dto.setFechaInicio(s.getFechaInicio());
                dto.setFechaFin(s.getFechaFin());
                dto.setEstado(s.getEstado());
                if (s.getEstudiante() != null && s.getEstudiante().getPersona() != null) {
                    var p = s.getEstudiante().getPersona();
                    String nombre = (p.getNombre() != null ? p.getNombre() : "") + " " + (p.getApellido() != null ? p.getApellido() : "");
                    dto.setNombreCompleto(nombre.trim());
                    dto.setFotoPerfil(p.getFotoUrl());
                    dto.setSexo(p.getSexo());
                } else {
                    dto.setNombreCompleto(null);
                    dto.setFotoPerfil(null);
                    dto.setSexo(null);
                }
                out.add(dto);
            }
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/vencidas")
    public ResponseEntity<?> marcarVencidas(@RequestParam("dias") int dias,
                                            @RequestParam(name = "actualizar", required = false, defaultValue = "false") boolean actualizar) {
        try {
            var all = solicitudAlojamientoService.readAll();
            if (all == null || all.isEmpty()) return new ResponseEntity<>(HttpStatus.NO_CONTENT);

            java.util.List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
            var now = DateTimeUtil.nowLima();
            for (var s : all) {
                if (s == null || s.getFechaSolicitud() == null) continue;
                long diff = ChronoUnit.DAYS.between(s.getFechaSolicitud().toLocalDate(), now.toLocalDate());
                // Only consider pending or reservada solicitudes for automatic vencido
                var estado = s.getEstado();
                boolean eligible = estado == SolicitudEstado.PENDIENTE || estado == SolicitudEstado.RESERVADA;
                if (diff >= dias && eligible) {
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", s.getId());
                    m.put("fechaSolicitud", s.getFechaSolicitud());
                    m.put("estadoActual", s.getEstado());
                    m.put("diasTranscurridos", diff);
                    out.add(m);
                    if (actualizar) {
                        try {
                            s.setEstado(SolicitudEstado.VENCIDO);
                            solicitudAlojamientoService.update(s);
                        } catch (Exception ex) {
                            // log and continue
                            logger.error("Error al marcar solicitud id={} como VENCIDO: {}", s.getId(), ex.getMessage());
                        }
                    }
                }
            }

            if (out.isEmpty()) return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            logger.error("Error en marcarVencidas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
