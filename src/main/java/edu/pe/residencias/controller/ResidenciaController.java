package edu.pe.residencias.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    @Autowired
    private edu.pe.residencias.service.ImagenResidenciaService imagenResidenciaService;
    @Autowired
    private edu.pe.residencias.service.ImagenHabitacionService imagenHabitacionService;
    @Autowired
    private edu.pe.residencias.service.CloudinaryService cloudinaryService;

    @Autowired
    private edu.pe.residencias.service.HabitacionService habitacionService;

    @Autowired
    private edu.pe.residencias.service.FavoritoService favoritoService;

    @Autowired
    private edu.pe.residencias.repository.ImagenHabitacionRepository imagenHabitacionRepository;
    @Autowired
    private edu.pe.residencias.repository.VistaRecienteRepository vistaRecienteRepository;

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

    // Helper: create habitacion entity from DTO after auth/ownership checks
    private ResponseEntity<?> createHabitacionHelper(HttpServletRequest request,
                                                    Long residenciaId,
                                                    edu.pe.residencias.model.dto.HabitacionUpdateDTO body) {
        try {
            // auth + owner/admin check
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Falta Authorization header");
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            var claims = jwtUtil.parseToken(token);
            String uid = claims.get("uid", String.class);
            if (uid == null || uid.isBlank()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido: uid faltante");
            var usuarioOpt = usuarioRepository.findByUuid(uid);
            if (usuarioOpt.isEmpty()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
            var usuario = usuarioOpt.get();

            var residenciaOpt = residenciaRepository.findById(residenciaId);
            if (residenciaOpt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            var residencia = residenciaOpt.get();
            if (!isOwnerOrAdmin(usuario, residencia)) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            // build entity
            edu.pe.residencias.model.entity.Habitacion h = new edu.pe.residencias.model.entity.Habitacion();
            h.setResidencia(residencia);
            // ensure visible by default when creating
            if (h.getVisible() == null) h.setVisible(true);
            if (body.getCodigoHabitacion() != null) h.setCodigoHabitacion(body.getCodigoHabitacion());
            if (body.getNombre() != null) h.setNombre(body.getNombre());
            if (body.getDepartamento() != null) h.setDepartamento(body.getDepartamento());
            if (body.getBanoPrivado() != null) h.setBanoPrivado(body.getBanoPrivado());
            if (body.getWifi() != null) h.setWifi(body.getWifi());
            if (body.getAmueblado() != null) h.setAmueblado(body.getAmueblado());
            if (body.getPiso() != null) h.setPiso(body.getPiso());
            if (body.getCapacidad() != null) h.setCapacidad(body.getCapacidad());
            if (body.getDescripcion() != null) h.setDescripcion(body.getDescripcion());
            if (body.getPermitir_mascotas() != null) h.setPermitir_mascotas(body.getPermitir_mascotas());
            if (body.getAgua() != null) h.setAgua(body.getAgua());
            if (body.getLuz() != null) h.setLuz(body.getLuz());
            if (body.getPrecioMensual() != null) h.setPrecioMensual(body.getPrecioMensual());
            if (body.getEstado() != null) {
                try { h.setEstado(edu.pe.residencias.model.enums.HabitacionEstado.fromString(body.getEstado())); }
                catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().body("estado inválido"); }
            }
            if (body.getDestacado() != null) h.setDestacado(body.getDestacado());
            if (body.getTerma() != null) h.setTerma(body.getTerma());

            edu.pe.residencias.model.entity.Habitacion created = habitacionService.create(h);

            // return HabitacionFullDTO
            edu.pe.residencias.model.dto.HabitacionFullDTO dto = new edu.pe.residencias.model.dto.HabitacionFullDTO();
            dto.setId(created.getId());
            dto.setCodigoHabitacion(created.getCodigoHabitacion());
            dto.setNombre(created.getNombre());
            dto.setDepartamento(created.getDepartamento());
            dto.setBanoPrivado(created.getBanoPrivado());
            dto.setWifi(created.getWifi());
            dto.setAmueblado(created.getAmueblado());
            dto.setPiso(created.getPiso());
            dto.setCapacidad(created.getCapacidad());
            dto.setDescripcion(created.getDescripcion());
            dto.setPermitir_mascotas(created.getPermitir_mascotas());
            dto.setAgua(created.getAgua());
            dto.setLuz(created.getLuz());
            dto.setTerma(created.getTerma());
            dto.setPrecioMensual(created.getPrecioMensual());
            dto.setEstado(created.getEstado() == null ? null : created.getEstado().name());
            dto.setDestacado(created.getDestacado());
            dto.setImagenes(java.util.List.of());

            return new ResponseEntity<>(dto, HttpStatus.CREATED);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            logger.error("Error creating habitacion under residencia", e);
            return new ResponseEntity<>("Error interno al crear habitación", HttpStatus.INTERNAL_SERVER_ERROR);
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

    // POST (application/json) - create habitacion with JSON body
    @PostMapping(path = "/{residenciaId}/habitaciones", consumes = {"application/json"})
    public ResponseEntity<?> createHabitacionJson(HttpServletRequest request,
                                                 @PathVariable("residenciaId") Long residenciaId,
                                                 @RequestBody edu.pe.residencias.model.dto.HabitacionUpdateDTO body) {
        // delegate to helper
        return createHabitacionHelper(request, residenciaId, body);
    }

    // (multipart upload removed) Only JSON creation is supported for habitaciones now.

    @GetMapping("/map")
    public ResponseEntity<?> getResidenciasForMap(HttpServletRequest request) {
        try {
            // require verified email users only
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Falta Authorization header");
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            var claims = jwtUtil.parseToken(token);
            String uid = claims.get("uid", String.class);
            if (uid == null || uid.isBlank()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido: uid faltante");
            var usuarioOpt = usuarioRepository.findByUuid(uid);
            if (usuarioOpt.isEmpty()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
            var usuario = usuarioOpt.get();
            if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body("Email no verificado");
            }

            java.util.List<edu.pe.residencias.model.dto.MapResidenciaDTO> list = residenciaService.findForMap();
            if (list.isEmpty()) return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            return new ResponseEntity<>(list, HttpStatus.OK);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            logger.warn("JWT parse error in /api/residencias/map", ex);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            logger.error("Error in /api/residencias/map", e);
            return new ResponseEntity<>("Error interno", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Public search endpoint with pagination. Example: /api/residencias/search?q=centro&page=0&size=10
    @GetMapping("/search")
    public ResponseEntity<?> searchResidencias(@org.springframework.web.bind.annotation.RequestParam(name = "q", required = false) String q,
                                               @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "0") int page,
                                               @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "10") int size) {
        try {
            if (page < 0) page = 0;
            if (size <= 0) size = 10;
            Pageable pageable = PageRequest.of(page, size);
            Page<Residencia> result = residenciaService.search(q, pageable);
            if (result == null || result.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error en /api/residencias/search", e);
            return new ResponseEntity<>("Error interno al buscar residencias", HttpStatus.INTERNAL_SERVER_ERROR);
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

            // habitaciones totals, disponibles y ocupadas (owner view)
            java.util.List<edu.pe.residencias.model.entity.Habitacion> allRooms = habitacionRepository.findByResidenciaId(r.getId());
            if (allRooms == null) allRooms = java.util.Collections.emptyList();
            int totalRooms = 0;
            int disponibles = 0;
            int ocupadas = 0;
            for (var h : allRooms) {
                try {
                    if (h == null) continue;
                    // skip eliminated rooms from totals
                    if (h.getEstado() != null && edu.pe.residencias.model.enums.HabitacionEstado.ELIMINADO.equals(h.getEstado())) continue;
                    totalRooms++;
                    boolean visible = !(h.getVisible() != null && !h.getVisible());
                    if (h.getEstado() != null && edu.pe.residencias.model.enums.HabitacionEstado.DISPONIBLE.equals(h.getEstado()) && visible) {
                        disponibles++;
                    }
                    if (h.getEstado() != null && edu.pe.residencias.model.enums.HabitacionEstado.NO_DISPONIBLE.equals(h.getEstado())) {
                        ocupadas++;
                    }
                } catch (Exception ignored) {}
            }
            dto.setHabitacionesTotales(totalRooms);
            dto.setHabitacionesOcupadas(ocupadas);

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
            // propietario: nombres y apellidos
            try {
                if (r.getUsuario() != null && r.getUsuario().getPersona() != null) {
                    var p = r.getUsuario().getPersona();
                    dto.setPropietarioNombre(p.getNombre());
                    dto.setPropietarioApellido(p.getApellido());
                }
            } catch (Exception ex) {
                logger.warn("No se pudo obtener persona del propietario para residencia id={}", r.getId(), ex);
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
                // skip residencias in ELIMINADO state for owner's simple list
                try {
                    if (r.getEstado() != null && r.getEstado().trim().equalsIgnoreCase("eliminado")) continue;
                } catch (Exception ignored) {}
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

                java.util.List<edu.pe.residencias.model.entity.Habitacion> allRoomsForCounts = habitacionRepository.findByResidenciaId(r.getId());
                if (allRoomsForCounts == null) allRoomsForCounts = java.util.Collections.emptyList();
                int totalRooms = 0;
                int disponibles = 0;
                int ocupadas = 0;
                for (var h : allRoomsForCounts) {
                    try {
                        if (h == null) continue;
                        // exclude eliminated rooms from totals
                        if (h.getEstado() != null && edu.pe.residencias.model.enums.HabitacionEstado.ELIMINADO.equals(h.getEstado())) continue;
                        totalRooms++;
                        boolean visible = !(h.getVisible() != null && !h.getVisible());
                        if (h.getEstado() != null && edu.pe.residencias.model.enums.HabitacionEstado.DISPONIBLE.equals(h.getEstado()) && visible) {
                            disponibles++;
                        }
                        if (h.getEstado() != null && edu.pe.residencias.model.enums.HabitacionEstado.NO_DISPONIBLE.equals(h.getEstado())) {
                            ocupadas++;
                        }
                    } catch (Exception ignored) {}
                }
                s.setHabitacionesTotales(totalRooms);
                s.setHabitacionesOcupadas(ocupadas);

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

                // propietario: nombres y apellidos
                try {
                    if (r.getUsuario() != null && r.getUsuario().getPersona() != null) {
                        var p = r.getUsuario().getPersona();
                        dto.setPropietarioNombre(p.getNombre());
                        dto.setPropietarioApellido(p.getApellido());
                    }
                } catch (Exception ex) {
                    logger.warn("No se pudo obtener persona del propietario para debug residencia id={}", r.getId(), ex);
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
                var usuario = usuarioOpt.get();
                if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body("Email no verificado");
                }
                residencia.setUsuario(usuario);
                // ensure visible by default when creating
                if (residencia.getVisible() == null) residencia.setVisible(true);
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

    @GetMapping("/{id}/card")
    public ResponseEntity<?> getResidenciaCard(@PathVariable("id") Long id) {
        try {
            var dto = residenciaService.getCardById(id);
            if (dto == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            return new ResponseEntity<>(dto, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error getting residencia card for id={}", id, e);
            return new ResponseEntity<>("Error interno", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}/habitaciones/preview")
    public ResponseEntity<?> getHabitacionesPreview(@PathVariable("id") Long id) {
        try {
            // verify residencia exists
            var residenciaOpt = residenciaRepository.findById(id);
            if (residenciaOpt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

            // try to resolve authenticated user (optional) and determine role (admin/owner/other)
            Long currentUserId = null;
            edu.pe.residencias.model.entity.Usuario requester = null;
            boolean isAdmin = false;
            boolean isOwner = false;
            var residencia = residenciaOpt.get();
            try {
                String authHeader = org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes() instanceof org.springframework.web.context.request.ServletRequestAttributes
                    ? ((org.springframework.web.context.request.ServletRequestAttributes)org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest().getHeader("Authorization")
                    : null;
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring("Bearer ".length()).trim();
                    var claims = jwtUtil.parseToken(token);
                    String uid = claims.get("uid", String.class);
                    if (uid != null && !uid.isBlank()) {
                        var uOpt = usuarioRepository.findByUuid(uid);
                        if (uOpt.isPresent()) {
                            requester = uOpt.get();
                            currentUserId = requester.getId();
                            try {
                                if (requester.getRol() != null && "admin".equalsIgnoreCase(requester.getRol().getNombre())) isAdmin = true;
                            } catch (Exception ignore) {}
                            try {
                                if (residencia.getUsuario() != null && residencia.getUsuario().getId() != null && requester.getId() != null && residencia.getUsuario().getId().equals(requester.getId())) isOwner = true;
                            } catch (Exception ignore) {}
                        }
                    }
                }
            } catch (Exception ignored) { }

            java.util.List<edu.pe.residencias.model.entity.Habitacion> hs = habitacionRepository.findByResidenciaId(id);
            if (hs == null) hs = java.util.Collections.emptyList();

            java.util.List<edu.pe.residencias.model.dto.HabitacionPreviewDTO> out = new java.util.ArrayList<>();
            for (var h : hs) {
                if (h == null) continue;
                try {
                    if (isAdmin) {
                        // admin sees everything
                    } else if (isOwner) {
                        // owner: hide only ELIMINADO
                        if (h.getEstado() != null && edu.pe.residencias.model.enums.HabitacionEstado.ELIMINADO.equals(h.getEstado())) continue;
                    } else {
                        // tenant / public: hide if not visible, NO_DISPONIBLE or ELIMINADO
                        if (h.getVisible() != null && !h.getVisible()) continue;
                        if (h.getEstado() != null) {
                            if (edu.pe.residencias.model.enums.HabitacionEstado.NO_DISPONIBLE.equals(h.getEstado()) ||
                                edu.pe.residencias.model.enums.HabitacionEstado.ELIMINADO.equals(h.getEstado())) {
                                continue;
                            }
                        }
                    }
                } catch (Exception ignored) {}
                String img = null;
                try {
                    java.util.List<edu.pe.residencias.model.entity.ImagenHabitacion> imgs = imagenHabitacionRepository.findByHabitacionId(h.getId());
                    if (imgs != null && !imgs.isEmpty()) {
                        // pick orden == 1 first, otherwise smallest orden
                        edu.pe.residencias.model.entity.ImagenHabitacion chosen = null;
                        for (var im : imgs) {
                            if (im == null) continue;
                            if (im.getOrden() != null && im.getOrden() == 1) { chosen = im; break; }
                            if (chosen == null) { chosen = im; continue; }
                            int oChosen = chosen.getOrden() == null ? Integer.MAX_VALUE : chosen.getOrden();
                            int oCur = im.getOrden() == null ? Integer.MAX_VALUE : im.getOrden();
                            if (oCur < oChosen) chosen = im;
                        }
                        if (chosen != null) img = chosen.getUrl();
                    }
                } catch (Exception ignored) {}

                String estadoLabel = null;
                try {
                    if (h.getEstado() != null) estadoLabel = h.getEstado().getLabel();
                } catch (Exception ignored) {}

                edu.pe.residencias.model.dto.HabitacionPreviewDTO dto = new edu.pe.residencias.model.dto.HabitacionPreviewDTO(
                    h.getId(), h.getNombre(), h.getPrecioMensual(), estadoLabel, img
                );
                try {
                    dto.setPiso(h.getPiso());
                } catch (Exception ignored) {}
                try {
                    dto.setCapacidad(h.getCapacidad());
                } catch (Exception ignored) {}
                try {
                    dto.setDestacado(h.getDestacado());
                } catch (Exception ignored) {}
                // determine favorito for authenticated user
                try {
                    if (currentUserId != null) {
                        boolean liked = favoritoService.isLiked(currentUserId, h.getId());
                        dto.setFavorito(liked);
                    } else {
                        dto.setFavorito(false);
                    }
                } catch (Exception ignored) {
                    dto.setFavorito(false);
                }
                out.add(dto);
            }

            // Put highlighted rooms first
            try {
                out.sort((a, b) -> {
                    boolean da = Boolean.TRUE.equals(a.getDestacado());
                    boolean db = Boolean.TRUE.equals(b.getDestacado());
                    if (da && !db) return -1;
                    if (!da && db) return 1;
                    return 0;
                });
            } catch (Exception ignore) {}

            return new ResponseEntity<>(out, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error in /api/residencias/{id}/habitaciones/preview", e);
            return new ResponseEntity<>("Error interno", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}/habitaciones")
    public ResponseEntity<?> getHabitacionesFullByResidencia(@PathVariable("id") Long id) {
        try {
            var residenciaOpt = residenciaRepository.findById(id);
            if (residenciaOpt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

            java.util.List<edu.pe.residencias.model.entity.Habitacion> hs = habitacionRepository.findByResidenciaId(id);
            if (hs == null) hs = java.util.Collections.emptyList();

            java.util.List<edu.pe.residencias.model.dto.HabitacionFullDTO> out = new java.util.ArrayList<>();
            for (var h : hs) {
                edu.pe.residencias.model.dto.HabitacionFullDTO dto = new edu.pe.residencias.model.dto.HabitacionFullDTO();
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
                dto.setPrecioMensual(h.getPrecioMensual());
                dto.setEstado(h.getEstado() == null ? null : (h.getEstado().name()));
                dto.setTerma(h.getTerma());
                dto.setDestacado(h.getDestacado());

                java.util.List<edu.pe.residencias.model.entity.ImagenHabitacion> imgs = imagenHabitacionRepository.findByHabitacionId(h.getId());
                java.util.List<String> imgDtos = new java.util.ArrayList<>();
                if (imgs != null) {
                    imgs.sort((a,b) -> {
                        int oa = a == null || a.getOrden() == null ? Integer.MAX_VALUE : a.getOrden();
                        int ob = b == null || b.getOrden() == null ? Integer.MAX_VALUE : b.getOrden();
                        return Integer.compare(oa, ob);
                    });
                    for (var im : imgs) {
                        if (im == null) continue;
                        imgDtos.add(im.getUrl());
                    }
                }
                dto.setImagenes(imgDtos);
                out.add(dto);
            }

            // Ensure highlighted rooms appear first
            try {
                out.sort((x,y) -> {
                    boolean dx = Boolean.TRUE.equals(x.getDestacado());
                    boolean dy = Boolean.TRUE.equals(y.getDestacado());
                    if (dx && !dy) return -1;
                    if (!dx && dy) return 1;
                    return 0;
                });
            } catch (Exception ignore) {}

            return new ResponseEntity<>(out, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error in /api/residencias/{id}/habitaciones (full)", e);
            return new ResponseEntity<>("Error interno", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET residencia status (owner or admin)
    @GetMapping("/{id}/status")
    public ResponseEntity<?> getResidenciaStatus(HttpServletRequest request, @PathVariable("id") Long id) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Falta Authorization header");
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            var claims = jwtUtil.parseToken(token);
            String uid = claims.get("uid", String.class);
            if (uid == null || uid.isBlank()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido: uid faltante");
            var usuarioOpt = usuarioRepository.findByUuid(uid);
            if (usuarioOpt.isEmpty()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
            var usuario = usuarioOpt.get();

            var residenciaOpt = residenciaRepository.findById(id);
            if (residenciaOpt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            var residencia = residenciaOpt.get();
            if (!isOwnerOrAdmin(usuario, residencia)) return new ResponseEntity<>(HttpStatus.FORBIDDEN);

            java.util.Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("id", residencia.getId());
            resp.put("estado", residencia.getEstado());
            return new ResponseEntity<>(resp, HttpStatus.OK);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            logger.error("Error in GET /api/residencias/{id}/status", e);
            return new ResponseEntity<>("Error interno", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // PUT residencia status - only allow ACTIVO, OCULTO, ELIMINADO (owner or admin)
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateResidenciaStatus(HttpServletRequest request, @PathVariable("id") Long id, @RequestBody java.util.Map<String, String> body) {
        try {
            String newEstado = body == null ? null : body.get("estado");
            if (newEstado == null || newEstado.isBlank()) return ResponseEntity.badRequest().body("estado requerido");
            String normalized = newEstado.trim();
            // Accept either names or display values
            boolean allowed = false;
            for (edu.pe.residencias.model.enums.ResidenciaEstado e : edu.pe.residencias.model.enums.ResidenciaEstado.values()) {
                if (e.name().equalsIgnoreCase(normalized) || e.toString().equalsIgnoreCase(normalized)) {
                    if (e == edu.pe.residencias.model.enums.ResidenciaEstado.ACTIVO || e == edu.pe.residencias.model.enums.ResidenciaEstado.OCULTO || e == edu.pe.residencias.model.enums.ResidenciaEstado.ELIMINADO) {
                        normalized = e.toString();
                        allowed = true;
                    }
                    break;
                }
            }
            if (!allowed) return ResponseEntity.badRequest().body("estado inválido; permitido: Activo, Oculto, Eliminado");

            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Falta Authorization header");
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            var claims = jwtUtil.parseToken(token);
            String uid = claims.get("uid", String.class);
            if (uid == null || uid.isBlank()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido: uid faltante");
            var usuarioOpt = usuarioRepository.findByUuid(uid);
            if (usuarioOpt.isEmpty()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
            var usuario = usuarioOpt.get();

            Optional<Residencia> opt = residenciaService.read(id);
            if (opt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            Residencia existing = opt.get();
            if (!isOwnerOrAdmin(usuario, existing)) return new ResponseEntity<>(HttpStatus.FORBIDDEN);

            existing.setEstado(normalized);
            Residencia updated = residenciaService.update(existing);
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            logger.error("Error in PUT /api/residencias/{id}/status", e);
            return new ResponseEntity<>("Error interno", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // PUT mark habitacion as ELIMINADO (owner or admin)
    @PutMapping("/{residenciaId}/habitaciones/{habitacionId}/eliminar")
    public ResponseEntity<?> eliminarHabitacion(HttpServletRequest request, @PathVariable("residenciaId") Long residenciaId, @PathVariable("habitacionId") Long habitacionId) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Falta Authorization header");
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            var claims = jwtUtil.parseToken(token);
            String uid = claims.get("uid", String.class);
            if (uid == null || uid.isBlank()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido: uid faltante");
            var usuarioOpt = usuarioRepository.findByUuid(uid);
            if (usuarioOpt.isEmpty()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
            var usuario = usuarioOpt.get();

            var residenciaOpt = residenciaRepository.findById(residenciaId);
            if (residenciaOpt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            var residencia = residenciaOpt.get();
            if (!isOwnerOrAdmin(usuario, residencia)) return new ResponseEntity<>(HttpStatus.FORBIDDEN);

            var habitacionOpt = habitacionRepository.findById(habitacionId);
            if (habitacionOpt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            var habitacion = habitacionOpt.get();
            if (habitacion.getResidencia() == null || habitacion.getResidencia().getId() == null || !habitacion.getResidencia().getId().equals(residenciaId)) {
                return ResponseEntity.badRequest().body("La habitación no pertenece a la residencia indicada");
            }

            // If the room was marked as destacado, unset it when deleting
            try {
                if (Boolean.TRUE.equals(habitacion.getDestacado())) {
                    habitacion.setDestacado(false);
                }
            } catch (Exception ignore) {}
            habitacion.setEstado(edu.pe.residencias.model.enums.HabitacionEstado.ELIMINADO);
            habitacionRepository.save(habitacion);
            return new ResponseEntity<>(habitacion, HttpStatus.OK);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            logger.error("Error in PUT eliminar habitacion", e);
            return new ResponseEntity<>("Error interno", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // PUT mark habitacion as DESTACADO (owner or admin)
    @PutMapping("/{residenciaId}/habitaciones/{habitacionId}/destacar")
    public ResponseEntity<?> destacarHabitacion(HttpServletRequest request, @PathVariable("residenciaId") Long residenciaId, @PathVariable("habitacionId") Long habitacionId) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Falta Authorization header");
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            var claims = jwtUtil.parseToken(token);
            String uid = claims.get("uid", String.class);
            if (uid == null || uid.isBlank()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido: uid faltante");
            var usuarioOpt = usuarioRepository.findByUuid(uid);
            if (usuarioOpt.isEmpty()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
            var usuario = usuarioOpt.get();

            var residenciaOpt = residenciaRepository.findById(residenciaId);
            if (residenciaOpt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            var residencia = residenciaOpt.get();
            if (!isOwnerOrAdmin(usuario, residencia)) return new ResponseEntity<>(HttpStatus.FORBIDDEN);

            var habitacionOpt = habitacionRepository.findById(habitacionId);
            if (habitacionOpt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            var habitacion = habitacionOpt.get();
            if (habitacion.getResidencia() == null || habitacion.getResidencia().getId() == null || !habitacion.getResidencia().getId().equals(residenciaId)) {
                return ResponseEntity.badRequest().body("La habitación no pertenece a la residencia indicada");
            }

            habitacion.setDestacado(Boolean.TRUE);
            // persist change
            habitacionRepository.save(habitacion);
            return new ResponseEntity<>(habitacion, HttpStatus.OK);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            logger.error("Error in PUT destacar habitacion", e);
            return new ResponseEntity<>("Error interno", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // PUT remove destacado from habitacion (owner or admin)
    @PutMapping("/{residenciaId}/habitaciones/{habitacionId}/quitar-destacado")
    public ResponseEntity<?> quitarDestacadoHabitacion(HttpServletRequest request, @PathVariable("residenciaId") Long residenciaId, @PathVariable("habitacionId") Long habitacionId) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Falta Authorization header");
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            var claims = jwtUtil.parseToken(token);
            String uid = claims.get("uid", String.class);
            if (uid == null || uid.isBlank()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido: uid faltante");
            var usuarioOpt = usuarioRepository.findByUuid(uid);
            if (usuarioOpt.isEmpty()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
            var usuario = usuarioOpt.get();

            var residenciaOpt = residenciaRepository.findById(residenciaId);
            if (residenciaOpt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            var residencia = residenciaOpt.get();
            if (!isOwnerOrAdmin(usuario, residencia)) return new ResponseEntity<>(HttpStatus.FORBIDDEN);

            var habitacionOpt = habitacionRepository.findById(habitacionId);
            if (habitacionOpt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            var habitacion = habitacionOpt.get();
            if (habitacion.getResidencia() == null || habitacion.getResidencia().getId() == null || !habitacion.getResidencia().getId().equals(residenciaId)) {
                return ResponseEntity.badRequest().body("La habitación no pertenece a la residencia indicada");
            }

            habitacion.setDestacado(Boolean.FALSE);
            habitacionRepository.save(habitacion);
            return new ResponseEntity<>(habitacion, HttpStatus.OK);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            logger.error("Error in PUT quitar destacado habitacion", e);
            return new ResponseEntity<>("Error interno", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET habitaciones stats (owner or admin)
    @GetMapping("/{id}/habitaciones/stats")
    public ResponseEntity<?> getHabitacionesStats(HttpServletRequest request, @PathVariable("id") Long id) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Falta Authorization header");
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            var claims = jwtUtil.parseToken(token);
            String uid = claims.get("uid", String.class);
            if (uid == null || uid.isBlank()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido: uid faltante");
            var usuarioOpt = usuarioRepository.findByUuid(uid);
            if (usuarioOpt.isEmpty()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
            var usuario = usuarioOpt.get();

            var residenciaOpt = residenciaRepository.findById(id);
            if (residenciaOpt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            var residencia = residenciaOpt.get();
            if (!isOwnerOrAdmin(usuario, residencia)) return new ResponseEntity<>(HttpStatus.FORBIDDEN);

            java.util.List<edu.pe.residencias.model.entity.Habitacion> hs = habitacionRepository.findByResidenciaId(id);
            if (hs == null) hs = java.util.Collections.emptyList();

            java.util.List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
            for (var h : hs) {
                if (h == null) continue;
                try {
                    // skip ELIMINADO rooms
                    if (h.getEstado() != null && edu.pe.residencias.model.enums.HabitacionEstado.ELIMINADO.equals(h.getEstado())) continue;
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", h.getId());
                    m.put("nombre", h.getNombre());
                    m.put("estado", h.getEstado() == null ? null : h.getEstado().name());
                    m.put("precioMensual", h.getPrecioMensual());
                    m.put("destacado", h.getDestacado());
                    m.put("createdAt", h.getCreatedAt());
                    // main image
                    String img = null;
                    java.util.List<edu.pe.residencias.model.entity.ImagenHabitacion> imgs = imagenHabitacionRepository.findByHabitacionId(h.getId());
                    if (imgs != null && !imgs.isEmpty()) {
                        edu.pe.residencias.model.entity.ImagenHabitacion chosen = null;
                        for (var im : imgs) {
                            if (im == null) continue;
                            if (im.getOrden() != null && im.getOrden() == 1) { chosen = im; break; }
                            if (chosen == null) { chosen = im; continue; }
                            int oChosen = chosen.getOrden() == null ? Integer.MAX_VALUE : chosen.getOrden();
                            int oCur = im.getOrden() == null ? Integer.MAX_VALUE : im.getOrden();
                            if (oCur < oChosen) chosen = im;
                        }
                        if (chosen != null) img = chosen.getUrl();
                    }
                    m.put("imagenPrincipal", img);
                    // likes
                    long likes = favoritoService.countLikes(h.getId());
                    m.put("likes", likes);
                    // vistas (use repository count)
                    long vistas = 0L;
                    try {
                        vistas = vistaRecienteRepository.countByHabitacionId(h.getId());
                    } catch (Exception ex) {
                        vistas = 0L;
                    }
                    m.put("vistas", vistas);
                    out.add(m);
                } catch (Exception ignored) {}
            }

            // sort: highlighted first, then by createdAt desc
            out.sort((a,b) -> {
                try {
                    boolean da = Boolean.TRUE.equals((Boolean)a.get("destacado"));
                    boolean db = Boolean.TRUE.equals((Boolean)b.get("destacado"));
                    if (da && !db) return -1;
                    if (!da && db) return 1;
                } catch (Exception ignored) {}
                java.time.LocalDateTime ta = (java.time.LocalDateTime) a.get("createdAt");
                java.time.LocalDateTime tb = (java.time.LocalDateTime) b.get("createdAt");
                if (ta == null && tb == null) return 0;
                if (ta == null) return 1;
                if (tb == null) return -1;
                return tb.compareTo(ta);
            });

            return new ResponseEntity<>(out, HttpStatus.OK);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            logger.error("Error in GET /api/residencias/{id}/habitaciones/stats", e);
            return new ResponseEntity<>("Error interno", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Residencia> delResidencia(HttpServletRequest request, @PathVariable("id") Long id) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body(null);
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            var claims = jwtUtil.parseToken(token);
            String uid = claims.get("uid", String.class);
            if (uid == null || uid.isBlank()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body(null);
            var usuarioOpt = usuarioRepository.findByUuid(uid);
            if (usuarioOpt.isEmpty()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body(null);
            var usuario = usuarioOpt.get();

            var residenciaOpt = residenciaRepository.findById(id);
            if (residenciaOpt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            var residencia = residenciaOpt.get();
            if (!isOwnerOrAdmin(usuario, residencia)) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            residenciaService.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            logger.warn("JWT parse error in DELETE /api/residencias/{id}", ex);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body(null);
        } catch (Exception e) {
            logger.error("Error deleting residencia id={}", id, e);
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateResidencia(HttpServletRequest request, @PathVariable("id") Long id, @RequestBody edu.pe.residencias.model.dto.ResidenciaUpdateDTO updateDTO) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Falta Authorization header");
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            var claims = jwtUtil.parseToken(token);
            String uid = claims.get("uid", String.class);
            if (uid == null || uid.isBlank()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido: uid faltante");
            var usuarioOpt = usuarioRepository.findByUuid(uid);
            if (usuarioOpt.isEmpty()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
            var usuario = usuarioOpt.get();

            Optional<Residencia> opt = residenciaService.read(id);
            if (opt.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            Residencia existing = opt.get();
            if (!isOwnerOrAdmin(usuario, existing)) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            // Update only the fields provided in the DTO (null means don't update)
            if (updateDTO.getNombre() != null) {
                existing.setNombre(updateDTO.getNombre());
            }
        if (updateDTO.getTipo() != null) {
            existing.setTipo(updateDTO.getTipo());
        }
        if (updateDTO.getReglamentoUrl() != null) {
            existing.setReglamentoUrl(updateDTO.getReglamentoUrl());
        }
        if (updateDTO.getDescripcion() != null) {
            existing.setDescripcion(updateDTO.getDescripcion());
        }
        if (updateDTO.getTelefonoContacto() != null) {
            existing.setTelefonoContacto(updateDTO.getTelefonoContacto());
        }
        if (updateDTO.getEmailContacto() != null) {
            existing.setEmailContacto(updateDTO.getEmailContacto());
        }
        if (updateDTO.getServicios() != null) {
            existing.setServicios(ServiciosUtil.normalizeServiciosText(updateDTO.getServicios()));
        }
        
            Residencia updatedResidencia = residenciaService.update(existing);
            return new ResponseEntity<>(updatedResidencia, HttpStatus.OK);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            logger.warn("JWT parse error in PUT /api/residencias/{id}", ex);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            logger.error("Unexpected error updating residencia id={}", id, e);
            return new ResponseEntity<>("Error interno al actualizar residencia", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

        @PutMapping("/{id}/imagenes")
        public ResponseEntity<?> updateResidenciaImagenes(HttpServletRequest request, @PathVariable("id") Long id, @RequestBody edu.pe.residencias.model.dto.ImagenesUpdateRequest body) {
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

                var residenciaOpt = residenciaRepository.findByIdWithImagenes(id);
                if (residenciaOpt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                var r = residenciaOpt.get();
                if (!isOwnerOrAdmin(usuario, r)) {
                    return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }

                java.util.List<String> urls = body == null || body.getImagenes() == null ? java.util.List.of() : body.getImagenes();
                java.util.List<edu.pe.residencias.model.entity.ImagenResidencia> updated = imagenResidenciaService.updateListForResidencia(id, urls);
                return new ResponseEntity<>(updated, HttpStatus.OK);
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
                logger.warn("JWT parse error in /api/residencias/{id}/imagenes", ex);
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido");
            } catch (Exception e) {
                logger.error("Unexpected error in updateResidenciaImagenes", e);
                return new ResponseEntity<>("Error interno al actualizar imágenes", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

    // NUEVO: Listado paginado de todas las residencias (ADMIN)
    @GetMapping("/admin/paginated")
    public ResponseEntity<?> getResidenciasPaginatedAdmin(HttpServletRequest request, Pageable pageable) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Falta Authorization header");
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            var claims = jwtUtil.parseToken(token);
            String uid = claims.get("uid", String.class);
            if (uid == null || uid.isBlank()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido: uid faltante");
            var usuarioOpt = usuarioRepository.findByUuid(uid);
            if (usuarioOpt.isEmpty()) return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
            var usuario = usuarioOpt.get();

            // require admin role
            try {
                if (usuario.getRol() == null || usuario.getRol().getNombre() == null || !"admin".equalsIgnoreCase(usuario.getRol().getNombre())) {
                    return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }
            } catch (Exception ignore) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

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
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            logger.warn("JWT parse error in GET /api/residencias/admin/paginated", ex);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            logger.error("Error in admin paginated residencias", e);
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
