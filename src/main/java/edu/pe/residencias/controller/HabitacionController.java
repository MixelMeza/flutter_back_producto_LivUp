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
import edu.pe.residencias.model.entity.Habitacion;
import edu.pe.residencias.model.entity.Residencia;
import edu.pe.residencias.service.HabitacionService;
import edu.pe.residencias.repository.ResidenciaRepository;
import edu.pe.residencias.repository.ImagenHabitacionRepository;
import edu.pe.residencias.model.dto.HabitacionFullDTO;
import jakarta.servlet.http.HttpServletRequest;
import edu.pe.residencias.model.dto.ImagenesUpdateRequest;

@RestController
@RequestMapping("/api/habitaciones")
public class HabitacionController {

    @Autowired
    private HabitacionService habitacionService;
    
    @Autowired
    private edu.pe.residencias.repository.HabitacionRepository habitacionRepository;
    
    @Autowired
    private ResidenciaRepository residenciaRepository;

    @Autowired
    private ImagenHabitacionRepository imagenHabitacionRepository;

    @Autowired
    private edu.pe.residencias.service.ImagenHabitacionService imagenHabitacionService;

    @Autowired
    private edu.pe.residencias.security.JwtUtil jwtUtil;

    @Autowired
    private edu.pe.residencias.repository.UsuarioRepository usuarioRepository;

    @Autowired
    private edu.pe.residencias.service.FavoritoService favoritoService;

    @GetMapping
    public ResponseEntity<?> readAll(@org.springframework.web.bind.annotation.RequestParam(value = "destacado", required = false) Boolean destacado,
                                     @org.springframework.web.bind.annotation.RequestParam(value = "limit", required = false) Integer limit,
                                     jakarta.servlet.http.HttpServletRequest request) {
        try {
            // If caller requests destacado=true, return a compact DTO list honoring limit and user favorites
            if (Boolean.TRUE.equals(destacado)) {
                List<Habitacion> habitaciones = habitacionService.readAll();
                java.util.stream.Stream<Habitacion> stream = habitaciones.stream().filter(h -> h != null && Boolean.TRUE.equals(h.getDestacado()) && h.getEstado() == edu.pe.residencias.model.enums.HabitacionEstado.DISPONIBLE);
                if (limit != null && limit > 0) stream = stream.limit(limit);

                // Determine user id from token (optional) - use AtomicReference to allow mutation
                java.util.concurrent.atomic.AtomicReference<Long> usuarioIdRef = new java.util.concurrent.atomic.AtomicReference<>(null);
                try {
                    String auth = request.getHeader("Authorization");
                    if (auth != null && auth.startsWith("Bearer ")) {
                        String token = auth.substring("Bearer ".length()).trim();
                        var claims = jwtUtil.parseToken(token);
                        String uid = claims.get("uid", String.class);
                        if (uid != null) {
                            var uopt = usuarioRepository.findByUuid(uid);
                            if (uopt.isPresent()) usuarioIdRef.set(uopt.get().getId());
                        }
                    }
                } catch (Exception ignore) {
                }

                java.util.List<edu.pe.residencias.model.dto.HabitacionDestacadaDTO> out = new java.util.ArrayList<>();
                stream.forEach(h -> {
                    try {
                        edu.pe.residencias.model.dto.HabitacionDestacadaDTO dto = new edu.pe.residencias.model.dto.HabitacionDestacadaDTO();
                        dto.setId(h.getId());
                        dto.setNombre(h.getNombre());
                        dto.setPrecioMensual(h.getPrecioMensual());
                        dto.setCapacidad(h.getCapacidad());
                        dto.setPiso(h.getPiso());
                        dto.setResidenciaNombre(h.getResidencia() != null ? h.getResidencia().getNombre() : null);

                        // first image
                        java.util.List<edu.pe.residencias.model.entity.ImagenHabitacion> imgs = imagenHabitacionRepository.findByHabitacionId(h.getId());
                        if (imgs != null && !imgs.isEmpty()) {
                            imgs.sort((a,b) -> {
                                int oa = a == null || a.getOrden() == null ? Integer.MAX_VALUE : a.getOrden();
                                int ob = b == null || b.getOrden() == null ? Integer.MAX_VALUE : b.getOrden();
                                return Integer.compare(oa, ob);
                            });
                            dto.setFotoUrl(imgs.get(0) != null ? imgs.get(0).getUrl() : null);
                        } else {
                            dto.setFotoUrl(null);
                        }

                        boolean fav = usuarioIdRef.get() != null && favoritoService.isLiked(usuarioIdRef.get(), h.getId());
                        dto.setFavorito(fav);

                        out.add(dto);
                    } catch (Exception ex) {
                        // skip problematic item
                    }
                });

                if (out.isEmpty()) return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                return ResponseEntity.ok(out);
            }

            // Default: return full list of Habitacion entities
            List<Habitacion> habitaciones = habitacionService.readAll();
            if (habitaciones.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(habitaciones, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<Habitacion> crear(@Valid @RequestBody Habitacion habitacion) {
        try {
            Habitacion h = habitacionService.create(habitacion);
            return new ResponseEntity<>(h, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getHabitacionId(@PathVariable("id") Long id) {
        try {
            Optional<Habitacion> opt = habitacionService.read(id);
            if (opt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            Habitacion h = opt.get();

            HabitacionFullDTO dto = new HabitacionFullDTO();
            dto.setId(h.getId());
            dto.setCodigoHabitacion(h.getCodigoHabitacion());
            dto.setNombre(h.getNombre());
            dto.setDepartamento(h.getDepartamento());
            dto.setBanoPrivado(h.getBanoPrivado());
            dto.setWifi(h.getWifi());
            dto.setAmueblado(h.getAmueblado());
            dto.setPiso(h.getPiso());
            dto.setCapacidad(h.getCapacidad());
            dto.setDescripcion(h.getDescripcion());
            dto.setPermitir_mascotas(h.getPermitir_mascotas());
            dto.setAgua(h.getAgua());
            dto.setLuz(h.getLuz());
            dto.setTerma(h.getTerma());
            dto.setPrecioMensual(h.getPrecioMensual());
            dto.setEstado(h.getEstado() == null ? null : h.getEstado().name());
            dto.setDestacado(h.getDestacado());

            java.util.List<edu.pe.residencias.model.entity.ImagenHabitacion> imgs = imagenHabitacionRepository.findByHabitacionId(h.getId());
            java.util.List<String> imgUrls = new java.util.ArrayList<>();
            if (imgs != null) {
                imgs.sort((a,b) -> {
                    int oa = a == null || a.getOrden() == null ? Integer.MAX_VALUE : a.getOrden();
                    int ob = b == null || b.getOrden() == null ? Integer.MAX_VALUE : b.getOrden();
                    return Integer.compare(oa, ob);
                });
                for (var im : imgs) {
                    if (im == null) continue;
                    imgUrls.add(im.getUrl());
                }
            }
            dto.setImagenes(imgUrls);

            return new ResponseEntity<>(dto, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/residencia/{residenciaId}")
    public ResponseEntity<List<Habitacion>> getHabitacionesPorResidencia(@PathVariable("residenciaId") Long residenciaId) {
        try {
            List<Habitacion> habitaciones = habitacionService.findByResidenciaId(residenciaId);
            if (habitaciones.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(habitaciones, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Habitacion> delHabitacion(@PathVariable("id") Long id) {
        try {
            habitacionService.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateHabitacion(@PathVariable("id") Long id, @RequestBody edu.pe.residencias.model.dto.HabitacionUpdateDTO update) {
        try {
            Optional<Habitacion> opt = habitacionService.read(id);
            if (opt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            Habitacion existing = opt.get();

            // Apply partial updates: only non-null fields from DTO overwrite entity
            if (update.getCodigoHabitacion() != null) existing.setCodigoHabitacion(update.getCodigoHabitacion());
            if (update.getNombre() != null) existing.setNombre(update.getNombre());
            if (update.getDepartamento() != null) existing.setDepartamento(update.getDepartamento());
            if (update.getBanoPrivado() != null) existing.setBanoPrivado(update.getBanoPrivado());
            if (update.getWifi() != null) existing.setWifi(update.getWifi());
            if (update.getAmueblado() != null) existing.setAmueblado(update.getAmueblado());
            if (update.getPiso() != null) existing.setPiso(update.getPiso());
            if (update.getCapacidad() != null) existing.setCapacidad(update.getCapacidad());
            if (update.getDescripcion() != null) existing.setDescripcion(update.getDescripcion());
            if (update.getPermitir_mascotas() != null) existing.setPermitir_mascotas(update.getPermitir_mascotas());
            if (update.getAgua() != null) existing.setAgua(update.getAgua());
            if (update.getLuz() != null) existing.setLuz(update.getLuz());
            if (update.getPrecioMensual() != null) existing.setPrecioMensual(update.getPrecioMensual());
            if (update.getEstado() != null) {
                try {
                    existing.setEstado(edu.pe.residencias.model.enums.HabitacionEstado.fromString(update.getEstado()));
                } catch (IllegalArgumentException ex) {
                    return ResponseEntity.badRequest().body("estado inválido");
                }
            }
            if (update.getDestacado() != null) existing.setDestacado(update.getDestacado());
            if (update.getTerma() != null) existing.setTerma(update.getTerma());

            Habitacion saved = habitacionService.update(existing);

            // Build HabitacionFullDTO to return same shape as GET /residencias/{id}/habitaciones
            HabitacionFullDTO dto = new HabitacionFullDTO();
            dto.setId(saved.getId());
            dto.setCodigoHabitacion(saved.getCodigoHabitacion());
            dto.setNombre(saved.getNombre());
            dto.setDepartamento(saved.getDepartamento());
            dto.setBanoPrivado(saved.getBanoPrivado());
            dto.setWifi(saved.getWifi());
            dto.setAmueblado(saved.getAmueblado());
            dto.setPiso(saved.getPiso());
            dto.setCapacidad(saved.getCapacidad());
            dto.setDescripcion(saved.getDescripcion());
            dto.setPermitir_mascotas(saved.getPermitir_mascotas());
            dto.setAgua(saved.getAgua());
            dto.setLuz(saved.getLuz());
            dto.setTerma(saved.getTerma());
            dto.setPrecioMensual(saved.getPrecioMensual());
            dto.setEstado(saved.getEstado() == null ? null : saved.getEstado().name());
            dto.setDestacado(saved.getDestacado());

            // images: ordered by orden asc, return list of URLs
            java.util.List<edu.pe.residencias.model.entity.ImagenHabitacion> imgs = imagenHabitacionRepository.findByHabitacionId(saved.getId());
            java.util.List<String> imgUrls = new java.util.ArrayList<>();
            if (imgs != null) {
                imgs.sort((a,b) -> {
                    int oa = a == null || a.getOrden() == null ? Integer.MAX_VALUE : a.getOrden();
                    int ob = b == null || b.getOrden() == null ? Integer.MAX_VALUE : b.getOrden();
                    return Integer.compare(oa, ob);
                });
                for (var im : imgs) {
                    if (im == null) continue;
                    imgUrls.add(im.getUrl());
                }
            }
            dto.setImagenes(imgUrls);

            return new ResponseEntity<>(dto, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error interno al actualizar habitación", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}/imagenes")
    public ResponseEntity<?> updateHabitacionImagenes(HttpServletRequest request, @PathVariable("id") Long id, @RequestBody ImagenesUpdateRequest body) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Falta Authorization header");
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            var claims = jwtUtil.parseToken(token);
            String uid = claims.get("uid", String.class);
            if (uid == null || uid.isBlank()) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido: uid faltante");
            }
            var usuarioOpt = usuarioRepository.findByUuid(uid);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
            }
            var usuario = usuarioOpt.get();

            var habitacionOpt = habitacionService.read(id);
            if (habitacionOpt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            var habitacion = habitacionOpt.get();
            var residencia = habitacion.getResidencia();
            if (residencia == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            if (!isOwnerOrAdmin(usuario, residencia)) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            java.util.List<String> urls = body == null || body.getImagenes() == null ? java.util.List.of() : body.getImagenes();
            java.util.List<edu.pe.residencias.model.entity.ImagenHabitacion> updated = imagenHabitacionService.updateListForHabitacion(id, urls);
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            return new ResponseEntity<>("Error interno al actualizar imágenes", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/residencia/{residenciaId}/disponibles")
    public ResponseEntity<?> getHabitacionesDisponibles(@PathVariable("residenciaId") Long residenciaId) {
        try {
            // Verificar si la residencia existe
            Optional<Residencia> residencia = residenciaRepository.findById(residenciaId);
            if (residencia.isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("No existe la residencia con ID: " + residenciaId);
            }
            
            // Buscar habitaciones disponibles
            List<Habitacion> habitaciones = habitacionRepository.findByResidenciaIdAndEstado(residenciaId, edu.pe.residencias.model.enums.HabitacionEstado.DISPONIBLE);
            
            if (habitaciones.isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("La residencia no tiene habitaciones disponibles");
            }
            
            return new ResponseEntity<>(habitaciones, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al buscar habitaciones: " + e.getMessage());
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
}
