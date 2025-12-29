package edu.pe.residencias.controller;

import edu.pe.residencias.model.entity.Residencia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.math.BigDecimal;
import java.text.Normalizer;

@RestController
@RequestMapping("/api")
public class SearchController {
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    private edu.pe.residencias.repository.ResidenciaRepository residenciaRepository;
    @Autowired
    private edu.pe.residencias.repository.HabitacionRepository habitacionRepository;
    @Autowired
    private edu.pe.residencias.service.FavoritoService favoritoService;
    @Autowired
    private edu.pe.residencias.repository.VistaRecienteRepository vistaRecienteRepository;

    @PostMapping(path = "/search", consumes = {"application/json"})
    public ResponseEntity<?> search(@RequestBody Map<String, Object> body) {
        try {
            String tipoBusqueda = body == null ? null : (String) body.get("tipoBusqueda");
            if (tipoBusqueda == null || (!"HABITACION".equalsIgnoreCase(tipoBusqueda) && !"RESIDENCIA".equalsIgnoreCase(tipoBusqueda))) {
                return ResponseEntity.badRequest().body("tipoBusqueda requerido: HABITACION o RESIDENCIA");
            }

            String q = body == null || body.get("q") == null ? null : body.get("q").toString().trim();
            String qNorm = normalize(q);

            BigDecimal lat = null, lng = null, radioKm = null;
            try { if (body.get("lat") != null) lat = toBigDecimal(body.get("lat")); } catch (Exception ignored) {}
            try { if (body.get("lng") != null) lng = toBigDecimal(body.get("lng")); } catch (Exception ignored) {}
            try {
                if (body.get("radioKm") != null) radioKm = toBigDecimal(body.get("radioKm"));
                // tolerate alternate client key
                if (radioKm == null && body.get("radiusKm") != null) radioKm = toBigDecimal(body.get("radiusKm"));
            } catch (Exception ignored) {}

            @SuppressWarnings("unchecked")
            Map<String,Object> residenciaFilters = body.get("residenciaFilters") == null ? Collections.emptyMap() : (Map<String,Object>) body.get("residenciaFilters");
            @SuppressWarnings("unchecked")
            Map<String,Object> habitacionFilters = body.get("habitacionFilters") == null ? Collections.emptyMap() : (Map<String,Object>) body.get("habitacionFilters");

            Double precioMin = null, precioMax = null;
            try { if (habitacionFilters.get("precioMin") != null) { BigDecimal bd = toBigDecimal(habitacionFilters.get("precioMin")); if (bd != null) precioMin = bd.doubleValue(); } } catch (Exception ignored) {}
            try { if (habitacionFilters.get("precioMax") != null) { BigDecimal bd = toBigDecimal(habitacionFilters.get("precioMax")); if (bd != null) precioMax = bd.doubleValue(); } } catch (Exception ignored) {}
            if (precioMin == null) { try { if (body.get("precioMin") != null) { BigDecimal bd = toBigDecimal(body.get("precioMin")); if (bd != null) precioMin = bd.doubleValue(); } } catch (Exception ignored) {} }
            if (precioMax == null) { try { if (body.get("precioMax") != null) { BigDecimal bd = toBigDecimal(body.get("precioMax")); if (bd != null) precioMax = bd.doubleValue(); } } catch (Exception ignored) {} }

            // Only apply a default max limit when the request is effectively "unfiltered" (only tipoBusqueda).
            // If the client provides any q/filters/radius/price constraints, return everything.
            boolean isUnfiltered = false;
            try {
                boolean qEmpty = (qNorm == null || qNorm.isBlank());
                boolean noResidenciaFilters = (residenciaFilters == null || residenciaFilters.isEmpty());
                boolean noHabitacionFilters = (habitacionFilters == null || habitacionFilters.isEmpty());
                boolean noGeo = (lat == null && lng == null && radioKm == null);
                boolean noPrice = (precioMin == null && precioMax == null);
                isUnfiltered = qEmpty && noResidenciaFilters && noHabitacionFilters && noGeo && noPrice;
            } catch (Exception ignored) {}

            // radius filter
            List<Residencia> allResidencias = residenciaRepository.findAll();
            List<Residencia> candidateResidencias = new ArrayList<>();
            if (lat != null && lng != null && radioKm != null) {
                for (var r : allResidencias) {
                    try {
                        if (r == null || r.getUbicacion() == null || r.getUbicacion().getLatitud() == null || r.getUbicacion().getLongitud() == null) continue;
                        BigDecimal rLat = r.getUbicacion().getLatitud();
                        BigDecimal rLng = r.getUbicacion().getLongitud();
                        double d = haversineKm(lat, lng, rLat, rLng);
                        if (d <= radioKm.doubleValue()) candidateResidencias.add(r);
                    } catch (Exception ignored) {}
                }
            } else candidateResidencias.addAll(allResidencias);

            List<Residencia> filteredResidencias = new ArrayList<>();
            for (var r : candidateResidencias) { if (applyResidenciaFilters(r, residenciaFilters)) filteredResidencias.add(r); }

            if ("HABITACION".equalsIgnoreCase(tipoBusqueda)) {
                // Return individual room results (one entry per habitacion). Residencia may repeat.
                List<Map<String,Object>> results = new ArrayList<>();
                for (var r : filteredResidencias) {
                    List<edu.pe.residencias.model.entity.Habitacion> hs = habitacionRepository.findByResidenciaId(r.getId());
                    if (hs == null) hs = Collections.emptyList();
                    for (var h : hs) {
                        if (h == null) continue;
                        try {
                            if (!applyHabitacionFilters(h, habitacionFilters, precioMin, precioMax)) continue;
                            int s = 0; if (qNorm != null && !qNorm.isEmpty()) { s = textMatchScore(qNorm, r, h); if (s == 0) continue; }
                            long likes = favoritoService.countLikes(h.getId());
                            long vistas = 0L; try { vistas = vistaRecienteRepository.countByHabitacionId(h.getId()); } catch (Exception ignored) {}
                            Map<String,Object> m = new HashMap<>();
                            m.put("residenciaId", r.getId());
                            m.put("habitacionId", h.getId());
                            if (r.getUbicacion() != null) { m.put("lat", r.getUbicacion().getLatitud()); m.put("lng", r.getUbicacion().getLongitud()); } else { m.put("lat", null); m.put("lng", null); }
                            // Destacado: Option A - only if residencia is destacado (ignore habitacion.destacado)
                            boolean destacado = Boolean.TRUE.equals(r == null ? null : r.getDestacado());
                            if (Boolean.TRUE.equals(h == null ? null : h.getDestacado()) && !destacado) {
                                System.out.println(String.format("Habitacion destacado ignored because residencia not destacado: residenciaId=%s habitacionId=%s", String.valueOf(r == null ? null : r.getId()), String.valueOf(h == null ? null : h.getId())));
                            }
                            m.put("destacado", destacado);
                            m.put("textScore", s);
                            m.put("likes", likes);
                            m.put("vistas", vistas);
                            m.put("createdAt", h.getCreatedAt() == null ? r.getCreatedAt() : h.getCreatedAt());
                            results.add(m);
                        } catch (Exception ignored) {}
                    }
                }
                results.sort(this::compareResults);
                if (isUnfiltered) results = limitResults(results, 100);
                return new ResponseEntity<>(results, HttpStatus.OK);
            } else {
                List<Map<String,Object>> results = new ArrayList<>();
                for (var r : filteredResidencias) {
                    List<edu.pe.residencias.model.entity.Habitacion> hs = habitacionRepository.findByResidenciaId(r.getId());
                    if (hs == null) hs = Collections.emptyList();
                    long likesSum = 0L; long vistasSum = 0L; int textScore = 0; boolean anyDestacado = (r != null && Boolean.TRUE.equals(r.getDestacado())); java.time.LocalDateTime latestCreated = null;

                    // If client provided habitacionFilters (or price constraints), require at least one matching room
                    boolean requireRoomMatch = false;
                    try {
                        requireRoomMatch = (body != null && body.get("habitacionFilters") != null && habitacionFilters != null && !habitacionFilters.isEmpty())
                                || (precioMin != null) || (precioMax != null);
                    } catch (Exception ignored) {}
                    int matchedRooms = 0;

                    // Match residencia itself first so residencias without habitaciones can match
                    if (qNorm != null && !qNorm.isEmpty()) {
                        textScore = textMatchScore(qNorm, r, null);
                    }

                    for (var h : hs) {
                        if (h == null) continue;
                        try {
                            // Apply habitacion filters when searching RESIDENCIA too
                            if (!applyHabitacionFilters(h, habitacionFilters, precioMin, precioMax)) continue;
                            matchedRooms++;
                            likesSum += favoritoService.countLikes(h.getId());
                            try { vistasSum += vistaRecienteRepository.countByHabitacionId(h.getId()); } catch (Exception ignored) {}
                            // do NOT propagate habitacion.destacado to residencia (Option A)
                            if (h.getCreatedAt() != null && (latestCreated == null || h.getCreatedAt().isAfter(latestCreated))) latestCreated = h.getCreatedAt();
                            if (qNorm != null && !qNorm.isEmpty()) textScore += textMatchScoreHabitacion(qNorm, h);
                        } catch (Exception ignored) {}
                    }
                    if (requireRoomMatch && matchedRooms == 0) continue;
                    if (qNorm != null && !qNorm.isEmpty() && textScore == 0) continue;
                    Map<String,Object> m = new HashMap<>();
                    m.put("residenciaId", r.getId());
                    if (r.getUbicacion() != null) { m.put("lat", r.getUbicacion().getLatitud()); m.put("lng", r.getUbicacion().getLongitud()); } else { m.put("lat", null); m.put("lng", null); }
                    // Destacado ONLY if residencia is destacado (Option A)
                    boolean finalDestacado = Boolean.TRUE.equals(r == null ? null : r.getDestacado());
                    m.put("destacado", finalDestacado); m.put("textScore", textScore); m.put("likes", likesSum); m.put("vistas", vistasSum); m.put("createdAt", latestCreated == null ? r.getCreatedAt() : latestCreated);
                    results.add(m);
                }
                results.sort(this::compareResults);
                if (isUnfiltered) results = limitResults(results, 100);
                return new ResponseEntity<>(results, HttpStatus.OK);
            }
        } catch (Exception e) {
            logger.error("Error in POST /api/search", e);
            return new ResponseEntity<>("Error interno", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<Map<String,Object>> limitResults(List<Map<String,Object>> results, int max) {
        if (results == null) return java.util.Collections.emptyList();
        if (max <= 0) return java.util.Collections.emptyList();
        if (results.size() <= max) return results;
        return new java.util.ArrayList<>(results.subList(0, max));
    }

    private int compareResults(Map<String,Object> a, Map<String,Object> b) {
        try {
            boolean da = Boolean.TRUE.equals((Boolean)a.get("destacado"));
            boolean db = Boolean.TRUE.equals((Boolean)b.get("destacado"));
            if (da != db) return da ? -1 : 1;
            int ta = (a.get("textScore") == null ? 0 : (int)a.get("textScore"));
            int tb = (b.get("textScore") == null ? 0 : (int)b.get("textScore"));
            if (ta != tb) return Integer.compare(tb, ta);
            long la = (a.get("likes") == null ? 0L : ((Number)a.get("likes")).longValue());
            long lb = (b.get("likes") == null ? 0L : ((Number)b.get("likes")).longValue());
            if (la != lb) return Long.compare(lb, la);
            long va = (a.get("vistas") == null ? 0L : ((Number)a.get("vistas")).longValue());
            long vb = (b.get("vistas") == null ? 0L : ((Number)b.get("vistas")).longValue());
            if (va != vb) return Long.compare(vb, va);
            java.time.LocalDateTime ca = (java.time.LocalDateTime)a.get("createdAt");
            java.time.LocalDateTime cb = (java.time.LocalDateTime)b.get("createdAt");
            if (ca == null && cb == null) return 0;
            if (ca == null) return 1;
            if (cb == null) return -1;
            return cb.compareTo(ca);
        } catch (Exception ignore) { return 0; }
    }

    // Helpers (copied from controller refactor)
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    private double haversineKm(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return Double.POSITIVE_INFINITY;
        return haversineKm(lat1.doubleValue(), lon1.doubleValue(), lat2.doubleValue(), lon2.doubleValue());
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return null;
        try {
            if (v instanceof BigDecimal) return (BigDecimal) v;
            if (v instanceof Number) return BigDecimal.valueOf(((Number) v).doubleValue());
            String s = v.toString();
            if (s.isBlank()) return null;
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInteger(Object v) {
        BigDecimal bd = toBigDecimal(v);
        if (bd == null) return null;
        try {
            return bd.intValue();
        } catch (Exception e) {
            return null;
        }
    }

    private Set<Integer> toIntegerSet(Object v) {
        if (v == null) return null;
        LinkedHashSet<Integer> out = new LinkedHashSet<>();
        try {
            if (v instanceof Collection) {
                for (Object o : (Collection<?>) v) {
                    Integer i = toInteger(o);
                    if (i == null) return null;
                    out.add(i);
                }
                return out;
            }
            if (v.getClass().isArray()) {
                Object[] arr = (Object[]) v;
                for (Object o : arr) {
                    Integer i = toInteger(o);
                    if (i == null) return null;
                    out.add(i);
                }
                return out;
            }
            // tolerate comma/space separated strings like "1,2,4" or "1 2 4"
            String s = v.toString();
            if (s == null || s.isBlank()) return null;
            if (s.contains(",") || s.contains(" ") || s.contains(";")) {
                String[] parts = s.split("[,;\\s]+");
                for (String p : parts) {
                    if (p == null || p.isBlank()) continue;
                    Integer i = toInteger(p.trim());
                    if (i == null) return null;
                    out.add(i);
                }
                return out;
            }
            Integer single = toInteger(v);
            if (single == null) return null;
            out.add(single);
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean applyResidenciaFilters(Residencia r, Map<String,Object> filters) {
        if (r == null) return false;
        // Exclude residencias con estado ELIMINADO/OCULTO
        try {
            if (r.getEstado() != null) {
                String est = r.getEstado().toString();
                if ("ELIMINADO".equalsIgnoreCase(est) || "OCULTO".equalsIgnoreCase(est)) return false;
            }
            // visible se trata como exclusión implícita (no como filtro)
            if (r.getVisible() == null || !r.getVisible()) return false;
        } catch (Exception ignored) {}
        if (filters == null || filters.isEmpty()) return true;
        try {
            Object tipo = filters.get("tipo"); if (tipo != null) { String t = tipo.toString().trim(); if (r.getTipo() == null || !r.getTipo().equalsIgnoreCase(t)) return false; }
            Object universidadId = filters.get("universidadId"); if (universidadId != null) { try { long uid = ((Number)universidadId).longValue(); if (r.getUniversidad() == null || r.getUniversidad().getId() == null || r.getUniversidad().getId() != uid) return false; } catch (Exception ignored) {} }
            Object estado = filters.get("estado"); if (estado != null) { String est = estado.toString().trim(); if (r.getEstado() == null || !r.getEstado().equalsIgnoreCase(est)) return false; }
            // Nota: 'visible' y 'destacado' no se usan como filtros aquí (visible se aplica como exclusión implícita; destacado solo para ordenamiento)
            Object servicios = filters.get("servicios");
            if (servicios != null && r.getServicios() != null) {
                try {
                    String rServ = normalize(r.getServicios());
                    List<String> required = new ArrayList<>();
                    if (servicios instanceof Collection) {
                        for (Object o : (Collection) servicios) if (o != null) required.add(o.toString().trim());
                    } else {
                        String s = servicios.toString();
                        if (s.contains(",")) {
                            for (String p : s.split(",")) required.add(p.trim());
                        } else {
                            required.add(s.trim());
                        }
                    }
                    for (String req : required) {
                        if (req == null || req.isBlank()) continue;
                        if (!residenciaHasService(rServ, req)) return false;
                    }
                } catch (Exception ignored) { return false; }
            }
        } catch (Exception ignored) {}
        return true;
    }

    private boolean applyHabitacionFilters(edu.pe.residencias.model.entity.Habitacion h, Map<String,Object> filters, Double precioMin, Double precioMax) {
        if (h == null) return false;
        // Excluir habitaciones con estado ELIMINADO y tratar 'visible' como exclusión implícita
        try {
            if (h.getEstado() != null && "ELIMINADO".equalsIgnoreCase(h.getEstado().name())) return false;
            if (h.getVisible() == null || !h.getVisible()) return false;
        } catch (Exception ignored) {}
            if (filters != null && !filters.isEmpty()) {
            try {
                // capacidad:
                // - if provided as scalar -> exact match
                // - if provided as list/CSV -> matches any of the exact values
                // Back-compat: capacity minimum can be expressed as capacidadMin
                Object capacidad = filters.get("capacidad");
                if (capacidad != null) {
                    Set<Integer> allowed = toIntegerSet(capacidad);
                    if (allowed == null || allowed.isEmpty()) return false; // invalid filter value provided
                    if (h.getCapacidad() == null || !allowed.contains(h.getCapacidad())) return false;
                }
                Object capacidadMin = filters.get("capacidadMin");
                if (capacidadMin != null) {
                    Integer cmin = toInteger(capacidadMin);
                    if (cmin == null) return false;
                    if (h.getCapacidad() == null || h.getCapacidad() < cmin) return false;
                }

                Object piso = filters.get("piso");
                if (piso != null) {
                    Integer p = toInteger(piso);
                    if (p == null) return false; // invalid filter value provided
                    if (h.getPiso() == null || !h.getPiso().equals(p)) return false;
                }
                Object amueblado = filters.get("amueblado"); if (amueblado != null) { try { boolean a = Boolean.parseBoolean(amueblado.toString()); if (h.getAmueblado() == null || h.getAmueblado() != a) return false; } catch (Exception ignored) {} }

                // Diferenciar filtros según filtro 'departamento' enviado por el cliente.
                // Si el cliente especifica `departamento` (boolean), entonces:
                //  - forzamos que la habitación coincida con ese tipo
                //  - aplicamos sólo los filtros de servicios correspondientes a ese tipo
                // Si no lo especifica (cualquiera), NO aplicamos filtros tipo-específicos (wifi/bañoPrivado vs agua/luz/terma).
                Object tipoDepartamentoFilter = filters.get("departamento");
                Boolean tipoDep = null;
                try { if (tipoDepartamentoFilter != null) tipoDep = Boolean.parseBoolean(tipoDepartamentoFilter.toString()); } catch (Exception ignored) {}

                // tolerate habitacionFilters.tipo = "CUARTO" | "DEPARTAMENTO"
                if (tipoDep == null) {
                    try {
                        Object tipo = filters.get("tipo");
                        if (tipo != null) {
                            String ts = tipo.toString().trim();
                            if (!ts.isBlank()) {
                                if ("DEPARTAMENTO".equalsIgnoreCase(ts)) tipoDep = true;
                                else if ("CUARTO".equalsIgnoreCase(ts) || "HABITACION".equalsIgnoreCase(ts)) tipoDep = false;
                            }
                        }
                    } catch (Exception ignored) {}
                }

                // gather service filters (if present)
                Boolean wantAgua = null; Boolean wantLuz = null; Boolean wantTerma = null; Boolean wantWifi = null; Boolean wantBanoPrivado = null;
                try { if (filters.get("agua") != null) wantAgua = Boolean.parseBoolean(filters.get("agua").toString()); } catch (Exception ignored) {}
                try { if (filters.get("luz") != null) wantLuz = Boolean.parseBoolean(filters.get("luz").toString()); } catch (Exception ignored) {}
                try { if (filters.get("terma") != null) wantTerma = Boolean.parseBoolean(filters.get("terma").toString()); } catch (Exception ignored) {}
                try { if (filters.get("wifi") != null) wantWifi = Boolean.parseBoolean(filters.get("wifi").toString()); } catch (Exception ignored) {}
                try { if (filters.get("banoPrivado") != null) wantBanoPrivado = Boolean.parseBoolean(filters.get("banoPrivado").toString()); } catch (Exception ignored) {}

                if (tipoDep != null) {
                    // enforce requested type
                    if (tipoDep && !Boolean.TRUE.equals(h.getDepartamento())) return false;
                    if (!tipoDep && Boolean.TRUE.equals(h.getDepartamento())) return false;

                    // apply only filters relevant to that type
                    if (tipoDep) {
                        if (wantAgua != null) { if (h.getAgua() == null || h.getAgua() != wantAgua) return false; }
                        if (wantLuz != null) { if (h.getLuz() == null || h.getLuz() != wantLuz) return false; }
                        if (wantTerma != null) { if (h.getTerma() == null || h.getTerma() != wantTerma) return false; }
                    } else {
                        if (wantWifi != null) { if (h.getWifi() == null || h.getWifi() != wantWifi) return false; }
                        if (wantBanoPrivado != null) { if (h.getBanoPrivado() == null || h.getBanoPrivado() != wantBanoPrivado) return false; }
                    }
                } else {
                    // tipo not specified: apply relevant service filters per room type
                    // - departamentos: agua/luz/terma
                    // - cuartos: wifi/banoPrivado
                    boolean anyServiceFilterProvided = (wantAgua != null) || (wantLuz != null) || (wantTerma != null) || (wantWifi != null) || (wantBanoPrivado != null);
                    if (anyServiceFilterProvided) {
                        boolean isDepartamento = Boolean.TRUE.equals(h.getDepartamento());
                        if (isDepartamento) {
                            if (wantAgua != null) { if (h.getAgua() == null || h.getAgua() != wantAgua) return false; }
                            if (wantLuz != null) { if (h.getLuz() == null || h.getLuz() != wantLuz) return false; }
                            if (wantTerma != null) { if (h.getTerma() == null || h.getTerma() != wantTerma) return false; }
                            // ignore cuarto-only filters
                        } else {
                            if (wantWifi != null) { if (h.getWifi() == null || h.getWifi() != wantWifi) return false; }
                            if (wantBanoPrivado != null) { if (h.getBanoPrivado() == null || h.getBanoPrivado() != wantBanoPrivado) return false; }
                            // ignore departamento-only filters
                        }
                    }
                }
                Object permitirMascotas = filters.get("permitir_mascotas");
                if (permitirMascotas == null) permitirMascotas = filters.get("permitirMascotas");
                if (permitirMascotas != null) { try { boolean pm = Boolean.parseBoolean(permitirMascotas.toString()); if (h.getPermitir_mascotas() == null || h.getPermitir_mascotas() != pm) return false; } catch (Exception ignored) {} }
                Object estado = filters.get("estado"); if (estado != null) { String est = estado.toString().trim(); if (h.getEstado() == null || !h.getEstado().name().equalsIgnoreCase(est)) return false; }
                // Nota: 'visible' y 'destacado' no se usan como filtros aquí (visible se aplica como exclusión implícita; destacado solo para ordenamiento)
            } catch (Exception ignored) {}
        }
        try { if (precioMin != null) { if (h.getPrecioMensual() == null || h.getPrecioMensual().doubleValue() < precioMin) return false; } if (precioMax != null) { if (h.getPrecioMensual() == null || h.getPrecioMensual().doubleValue() > precioMax) return false; } } catch (Exception ignored) {}
        return true;
    }

    private int textMatchScore(String qNorm, Residencia r, edu.pe.residencias.model.entity.Habitacion h) {
        if (qNorm == null || qNorm.isBlank()) return 0;
        int score = 0;
        try {
            String[] terms = qNorm.split("\\s+");
            for (String t : terms) {
                if (t == null || t.isBlank()) continue;
                String term = t.trim();
                try {
                    if (r != null) {
                        if (r.getNombre() != null && normalize(r.getNombre()).contains(term)) { score += 2; System.out.println(String.format("textMatchScore: residenciaId=%s term='%s' field=nombre +2", String.valueOf(r == null ? null : r.getId()), term)); }
                        if (r.getUbicacion() != null) {
                            var u = r.getUbicacion();
                            if (u.getDireccion() != null && normalize(u.getDireccion()).contains(term)) { score += 3; System.out.println(String.format("textMatchScore: residenciaId=%s term='%s' field=direccion +3", String.valueOf(r == null ? null : r.getId()), term)); }
                            if (u.getDistrito() != null && normalize(u.getDistrito()).contains(term)) { score += 2; System.out.println(String.format("textMatchScore: residenciaId=%s term='%s' field=distrito +2", String.valueOf(r == null ? null : r.getId()), term)); }
                            if (u.getProvincia() != null && normalize(u.getProvincia()).contains(term)) { score += 2; System.out.println(String.format("textMatchScore: residenciaId=%s term='%s' field=provincia +2", String.valueOf(r == null ? null : r.getId()), term)); }
                            if (u.getDepartamento() != null && normalize(u.getDepartamento()).contains(term)) { score += 1; System.out.println(String.format("textMatchScore: residenciaId=%s term='%s' field=departamento +1", String.valueOf(r == null ? null : r.getId()), term)); }
                            if (u.getPais() != null && normalize(u.getPais()).contains(term)) { score += 1; System.out.println(String.format("textMatchScore: residenciaId=%s term='%s' field=pais +1", String.valueOf(r == null ? null : r.getId()), term)); }
                        }
                    }
                    if (h != null) {
                        if (h.getNombre() != null && normalize(h.getNombre()).contains(term)) { score += 3; System.out.println(String.format("textMatchScore: habitacionId=%s term='%s' field=habitacion_nombre +3", String.valueOf(h == null ? null : h.getId()), term)); }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        try { System.out.println(String.format("textMatchScore total: residenciaId=%s habitacionId=%s q='%s' score=%d", String.valueOf(r == null ? null : r.getId()), String.valueOf(h == null ? null : h.getId()), qNorm, score)); } catch (Exception ignored) {}
        return score;
    }

    private int textMatchScoreHabitacion(String qNorm, edu.pe.residencias.model.entity.Habitacion h) {
        if (qNorm == null || qNorm.isBlank() || h == null) return 0;
        int score = 0;
        try {
            String[] terms = qNorm.split("\\s+");
            for (String t : terms) {
                if (t == null || t.isBlank()) continue;
                String term = t.trim();
                try {
                    if (h.getNombre() != null && normalize(h.getNombre()).contains(term)) { score += 3; System.out.println(String.format("textMatchScoreHabitacion: habitacionId=%s term='%s' field=nombre +3", String.valueOf(h == null ? null : h.getId()), term)); }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        try { System.out.println(String.format("textMatchScoreHabitacion total: habitacionId=%s q='%s' score=%d", String.valueOf(h == null ? null : h.getId()), qNorm, score)); } catch (Exception ignored) {}
        return score;
    }

    private String normalize(String s) {
        if (s == null) return "";
        String low = s.toLowerCase();
        String norm = Normalizer.normalize(low, Normalizer.Form.NFD);
        return norm.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").trim();
    }

    private boolean residenciaHasService(String rServNormalized, String req) {
        if (rServNormalized == null || req == null) return false;
        String q = normalize(req);
        // Split residencia services into tokens by common separators (keep multi-word tokens)
        String[] parts = rServNormalized.split("[,;|/\\\\n]");
        Set<String> tokens = new HashSet<>();
        for (String p : parts) {
            String t = p == null ? "" : p.trim();
            if (!t.isEmpty()) tokens.add(t);
        }

        // horario/recepción/atención variants
        if (q.contains("horario") || q.contains("recepcion") || q.contains("atencion")) {
            for (String t : tokens) if (t.contains("horario") || t.contains("recepcion") || t.contains("atencion")) return true;
            return false;
        }

        // synonyms mapping (including wifi)
        List<String> syns;
        if (q.contains("lav") || q.contains("lavad") || q.contains("lavander")) syns = Arrays.asList("lavadora", "lavanderia", "lavanderia", "lavado");
        else if (q.contains("pens") || q.contains("pensi") || q.contains("comedor")) syns = Arrays.asList("pension", "pension", "comedor", "pensiones");
        else if (q.contains("limp") || q.contains("limpieza")) syns = Arrays.asList("limpieza", "servicio de limpieza", "limpieza incluida");
        else if (q.contains("park") || q.contains("estacion") || q.contains("parqueo")) syns = Arrays.asList("parking", "estacionamiento", "parqueo");
        else if (q.contains("sala") || q.contains("estudio")) syns = Arrays.asList("sala de estudio", "sala estudio", "estudio");
        else if (q.contains("seg") || q.contains("vigil") || q.contains("camar")) syns = Arrays.asList("seguridad", "vigilancia", "camaras", "guardia");
        else if (q.contains("wifi") || q.contains("wi-fi")) syns = Arrays.asList("wifi", "wi fi", "conexion wifi");
        else syns = Arrays.asList(q);

        for (String s : syns) {
            String sn = normalize(s);
            for (String t : tokens) {
                if (t.contains(sn) || sn.contains(t)) return true;
            }
        }
        // fallback: contains check on whole string
        for (String s : syns) if (rServNormalized.contains(normalize(s))) return true;
        return false;
    }
}
