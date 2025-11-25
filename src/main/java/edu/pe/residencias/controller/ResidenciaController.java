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
import edu.pe.residencias.model.entity.Residencia;
import edu.pe.residencias.model.enums.ResidenciaEstado;
import edu.pe.residencias.service.ResidenciaService;
import edu.pe.residencias.util.ServiciosUtil;
import edu.pe.residencias.repository.UbicacionRepository;
import edu.pe.residencias.repository.UniversidadRepository;
import edu.pe.residencias.repository.UsuarioRepository;
import edu.pe.residencias.repository.ResidenciaRepository;
import edu.pe.residencias.repository.HabitacionRepository;
import edu.pe.residencias.repository.ContratoRepository;
import edu.pe.residencias.repository.PagoRepository;
import edu.pe.residencias.security.JwtUtil;
import edu.pe.residencias.model.dto.ResidenciaOwnerDTO;
import edu.pe.residencias.model.dto.SimpleResidenciaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.pe.residencias.model.dto.UbicacionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import edu.pe.residencias.model.dto.ResidenciaAdminDTO;
import edu.pe.residencias.model.entity.ImagenResidencia;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.ArrayList;


@RestController
@RequestMapping("/api/residencias")
public class ResidenciaController {
    private static final Logger logger = LoggerFactory.getLogger(ResidenciaController.class);
    
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

    @Autowired
    private ResidenciaRepository residenciaRepository;
    
    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private ContratoRepository contratoRepository;

    @Autowired
    private PagoRepository pagoRepository;

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

    @GetMapping("/mine")
    public ResponseEntity<?> getMyResidenciaDetail(HttpServletRequest request, @org.springframework.web.bind.annotation.RequestParam(name = "id") Long residenciaId) {
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

            // Fetch residencia by id with images initialized
            var residenciaOpt = residenciaRepository.findByIdWithImagenes(residenciaId);
            if (residenciaOpt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            var r = residenciaOpt.get();

            // Check owner
            if (r.getUsuario() == null || !r.getUsuario().getId().equals(usuario.getId())) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            // Build detailed DTO
            ResidenciaOwnerDTO dto = new ResidenciaOwnerDTO();
            dto.setId(r.getId());
            dto.setNombre(r.getNombre());
            dto.setTipo(r.getTipo());
            dto.setCantidadHabitaciones(r.getCantidadHabitaciones());
            dto.setDescripcion(r.getDescripcion());
            dto.setTelefonoContacto(r.getTelefonoContacto());
            dto.setEmailContacto(r.getEmailContacto());
            dto.setServicios(r.getServicios());
            dto.setEstado(r.getEstado());
            dto.setCreatedAt(r.getCreatedAt());

            var u = r.getUbicacion();
            if (u != null) {
                UbicacionDTO udto = new UbicacionDTO(
                    u.getId(), u.getDireccion(), u.getDepartamento(), u.getDistrito(), u.getProvincia(), u.getPais(), u.getLatitud(), u.getLongitud()
                );
                dto.setUbicacion(udto);
            }

            if (r.getImagenesResidencia() != null) {
                List<String> imgs = r.getImagenesResidencia().stream()
                    .filter(im -> im != null)
                    .sorted(Comparator.comparing(im -> im.getOrden() == null ? Integer.MAX_VALUE : im.getOrden()))
                    .map(ImagenResidencia::getUrl)
                    .collect(Collectors.toList());
                dto.setImagenes(imgs);
            }

            // habitaciones totals and occupied
            long totalRooms = habitacionRepository.countByResidenciaId(r.getId());
            long occupiedRooms = contratoRepository.countDistinctHabitacionesByResidenciaIdAndEstado(r.getId(), "vigente");
            dto.setHabitacionesTotales((int) totalRooms);
            dto.setHabitacionesOcupadas((int) occupiedRooms);

            // ingresos
            java.util.List<String> acceptedStates = java.util.Arrays.asList("pagado", "completado", "paid");
            java.math.BigDecimal ingresos = pagoRepository.sumPagosByResidenciaIdAndEstados(r.getId(), acceptedStates);
            dto.setIngresos(ingresos == null ? java.math.BigDecimal.ZERO : ingresos);

            // Sync cantidadHabitaciones if needed (non-blocking)
            try {
                Integer cantidad = r.getCantidadHabitaciones();
                int totalRoomsInt = (int) totalRooms;
                if (cantidad == null || cantidad.intValue() != totalRoomsInt) {
                    r.setCantidadHabitaciones(totalRoomsInt);
                    try {
                        residenciaService.update(r);
                    } catch (Exception ex) {
                        logger.warn("Failed to update cantidadHabitaciones for residencia id={}", r.getId(), ex);
                    }
                }
            } catch (Exception ex) {
                logger.warn("Error while syncing cantidadHabitaciones for residencia id={}", r.getId(), ex);
            }

            return new ResponseEntity<>(dto, HttpStatus.OK);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            logger.warn("JWT parse error in /api/residencias/mine", ex);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            logger.error("Unexpected error in /api/residencias/mine", e);
            return new ResponseEntity<>("Error interno al obtener residencias del propietario", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Simplified owner listing: minimal fields for owner's residencias
    @GetMapping("/mine/simple")
    public ResponseEntity<?> getMyResidenciasSimple(HttpServletRequest request) {
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

            List<edu.pe.residencias.model.entity.Residencia> residencias = residenciaRepository.findByUsuarioIdWithImagenes(usuario.getId());
            if (residencias.isEmpty()) return new ResponseEntity<>(HttpStatus.NO_CONTENT);

            List<SimpleResidenciaDTO> list = new ArrayList<>();
            for (var r : residencias) {
                SimpleResidenciaDTO s = new SimpleResidenciaDTO();
                s.setId(r.getId());
                s.setNombre(r.getNombre());
                s.setTipo(r.getTipo());
                s.setEstado(r.getEstado());

                var u = r.getUbicacion();
                if (u != null) {
                    UbicacionDTO udto = new UbicacionDTO(
                        u.getId(), u.getDireccion(), u.getDepartamento(), u.getDistrito(), u.getProvincia(), u.getPais(), u.getLatitud(), u.getLongitud()
                    );
                    s.setUbicacion(udto);
                }

                if (r.getImagenesResidencia() != null) {
                    String firstImg = r.getImagenesResidencia().stream()
                        .filter(im -> im != null)
                        .sorted(Comparator.comparing(im -> im.getOrden() == null ? Integer.MAX_VALUE : im.getOrden()))
                        .map(ImagenResidencia::getUrl)
                        .findFirst()
                        .orElse(null);
                    s.setImagen(firstImg);
                } else {
                    s.setImagen(null);
                }

                long totalRooms = habitacionRepository.countByResidenciaId(r.getId());
                long occupiedRooms = contratoRepository.countDistinctHabitacionesByResidenciaIdAndEstado(r.getId(), "vigente");
                s.setHabitacionesTotales((int) totalRooms);
                s.setHabitacionesOcupadas((int) occupiedRooms);

                java.util.List<String> acceptedStates = java.util.Arrays.asList("pagado", "completado", "paid");
                java.math.BigDecimal ingresos = pagoRepository.sumPagosByResidenciaIdAndEstados(r.getId(), acceptedStates);
                s.setIngresos(ingresos == null ? java.math.BigDecimal.ZERO : ingresos);

                list.add(s);
            }

            return new ResponseEntity<>(list, HttpStatus.OK);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            logger.warn("JWT parse error in /api/residencias/mine/simple", ex);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            logger.error("Unexpected error in /api/residencias/mine/simple", e);
            return new ResponseEntity<>("Error interno al obtener residencias del propietario", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // DEBUG endpoint: fetch residencias by usuario id without JWT (useful for local debugging)
    @GetMapping("/owner/{usuarioId}/debug")
    public ResponseEntity<?> getResidenciasByOwnerDebug(@PathVariable("usuarioId") Long usuarioId) {
        try {
            var usuarioOpt = usuarioRepository.findById(usuarioId == 0 ? -1L : usuarioId);
            if (usuarioId == 0) {
                // pick first user if user passed 0
                usuarioOpt = usuarioRepository.findAll().stream().findFirst();
            }
            if (usuarioOpt.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            var usuario = usuarioOpt.get();
            List<edu.pe.residencias.model.entity.Residencia> residencias = residenciaRepository.findByUsuarioIdWithImagenes(usuario.getId());
            if (residencias.isEmpty()) return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            List<ResidenciaOwnerDTO> dtos = new ArrayList<>();
            for (var r : residencias) {
                ResidenciaOwnerDTO dto = new ResidenciaOwnerDTO();
                dto.setId(r.getId());
                dto.setNombre(r.getNombre());
                dto.setTipo(r.getTipo());
                dto.setCantidadHabitaciones(r.getCantidadHabitaciones());
                dto.setDescripcion(r.getDescripcion());
                dto.setTelefonoContacto(r.getTelefonoContacto());
                dto.setEmailContacto(r.getEmailContacto());
                dto.setServicios(r.getServicios());
                dto.setEstado(r.getEstado());
                dto.setCreatedAt(r.getCreatedAt());

                var u = r.getUbicacion();
                if (u != null) {
                    UbicacionDTO udto = new UbicacionDTO(
                        u.getId(), u.getDireccion(), u.getDepartamento(), u.getDistrito(), u.getProvincia(), u.getPais(), u.getLatitud(), u.getLongitud()
                    );
                    dto.setUbicacion(udto);
                }

                if (r.getImagenesResidencia() != null) {
                    List<String> imgs = r.getImagenesResidencia().stream()
                        .filter(im -> im != null)
                        .sorted(Comparator.comparing(im -> im.getOrden() == null ? Integer.MAX_VALUE : im.getOrden()))
                        .map(ImagenResidencia::getUrl)
                        .collect(Collectors.toList());
                    dto.setImagenes(imgs);
                }

                // Ensure cantidadHabitaciones in entity matches actual count
                long totalRoomsDebug = habitacionRepository.countByResidenciaId(r.getId());
                int totalRoomsDebugInt = (int) totalRoomsDebug;
                try {
                    Integer cantidad = r.getCantidadHabitaciones();
                    if (cantidad == null || cantidad.intValue() != totalRoomsDebugInt) {
                        r.setCantidadHabitaciones(totalRoomsDebugInt);
                        try {
                            residenciaService.update(r);
                        } catch (Exception ex) {
                            logger.warn("Failed to update cantidadHabitaciones for debug residencia id={}", r.getId(), ex);
                        }
                    }
                } catch (Exception ex) {
                    logger.warn("Error while syncing cantidadHabitaciones for debug residencia id={}", r.getId(), ex);
                }

                dtos.add(dto);
            }
            return new ResponseEntity<>(dtos, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error in debug owner endpoint", e);
            return new ResponseEntity<>("Error interno", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(HttpServletRequest request, @Valid @RequestBody Residencia residencia) {
        try {
            // Validate and set estado default
            if (residencia.getEstado() == null || residencia.getEstado().isBlank()) {
                residencia.setEstado(ResidenciaEstado.ACTIVO.toString());
            } else if (!ResidenciaEstado.isValid(residencia.getEstado())) {
                return ResponseEntity.badRequest().body("estado inválido");
            }

            // Normalize servicios field (simple comma-separated words)
            residencia.setServicios(ServiciosUtil.normalizeServiciosText(residencia.getServicios()));

            // Assign universidad_id = 1 if not provided
            if (residencia.getUniversidad() == null) {
                universidadRepository.findById(1L).ifPresent(residencia::setUniversidad);
            }

            // Basic validation: nombre
            if (residencia.getNombre() == null || residencia.getNombre().isBlank()) {
                return ResponseEntity.badRequest().body("nombre es requerido");
            }

            // Assign usuario from token (Authorization: Bearer <token>) - required because DB enforces non-null usuario_id
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
                residencia.setUsuario(usuarioOpt.get());
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido");
            } catch (Exception ex) {
                return new ResponseEntity<>("Error interno al validar token", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // If ubicacion is provided as nested object, validate then persist it
            var savedUb = (edu.pe.residencias.model.entity.Ubicacion) null;
            if (residencia.getUbicacion() != null) {
                var ub = residencia.getUbicacion();
                boolean hasCoords = ub.getLatitud() != null && ub.getLongitud() != null;
                boolean hasAddress = ub.getDireccion() != null && !ub.getDireccion().isBlank();
                if (!hasCoords && !hasAddress) {
                    return ResponseEntity.badRequest().body("ubicacion inválida: requiere lat/lon o direccion");
                }
                // Save ubicacion (will set id)
                savedUb = ubicacionRepository.save(ub);
                residencia.setUbicacion(savedUb);
            }

            try {
                Residencia r = residenciaService.create(residencia);
                return new ResponseEntity<>(r, HttpStatus.CREATED);
            } catch (Exception ex) {
                // If we created a ubicacion but residencia creation failed, delete the ubicacion to avoid orphans
                try {
                    if (savedUb != null && savedUb.getId() != null) {
                        ubicacionRepository.deleteById(savedUb.getId());
                    }
                } catch (Exception ignore) {}
                throw ex;
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Error interno al crear residencia", HttpStatus.INTERNAL_SERVER_ERROR);
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

    // NUEVO: Listado paginado de todas las residencias (ADMIN)
    @GetMapping("/admin/paginated")
    public ResponseEntity<?> getResidenciasPaginatedAdmin(Pageable pageable) {
        try {
            Page<Residencia> residenciasPage = residenciaService.findAllPaginated(pageable);
            if (residenciasPage.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            // Convertir a DTOs
            List<ResidenciaAdminDTO> dtos = residenciaService.mapToResidenciaAdminDTOs(residenciasPage.getContent());
            
            // Crear respuesta con paginación
            java.util.HashMap<String, Object> response = new java.util.HashMap<>();
            response.put("content", dtos);
            response.put("totalElements", residenciasPage.getTotalElements());
            response.put("totalPages", residenciasPage.getTotalPages());
            response.put("currentPage", residenciasPage.getNumber());
            response.put("pageSize", residenciasPage.getSize());
            response.put("hasNext", residenciasPage.hasNext());
            response.put("hasPrevious", residenciasPage.hasPrevious());

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
