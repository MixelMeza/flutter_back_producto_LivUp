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

            BigDecimal lat = null, lng = null, radioKm = null;
            try { if (body.get("lat") != null) lat = toBigDecimal(body.get("lat")); } catch (Exception ignored) {}
            try { if (body.get("lng") != null) lng = toBigDecimal(body.get("lng")); } catch (Exception ignored) {}
            try { if (body.get("radioKm") != null) radioKm = toBigDecimal(body.get("radioKm")); } catch (Exception ignored) {}

            @SuppressWarnings("unchecked")
            Map<String,Object> residenciaFilters = body.get("residenciaFilters") == null ? Collections.emptyMap() : (Map<String,Object>) body.get("residenciaFilters");
            @SuppressWarnings("unchecked")
            Map<String,Object> habitacionFilters = body.get("habitacionFilters") == null ? Collections.emptyMap() : (Map<String,Object>) body.get("habitacionFilters");

            Double precioMin = null, precioMax = null;
            try { if (habitacionFilters.get("precioMin") != null) precioMin = ((Number)habitacionFilters.get("precioMin")).doubleValue(); } catch (Exception ignored) {}
            try { if (habitacionFilters.get("precioMax") != null) precioMax = ((Number)habitacionFilters.get("precioMax")).doubleValue(); } catch (Exception ignored) {}
            if (precioMin == null) { try { if (body.get("precioMin") != null) precioMin = ((Number)body.get("precioMin")).doubleValue(); } catch (Exception ignored) {} }
            if (precioMax == null) { try { if (body.get("precioMax") != null) precioMax = ((Number)body.get("precioMax")).doubleValue(); } catch (Exception ignored) {} }

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
                            int s = 0; if (q != null && !q.isBlank()) { s = textMatchScore(q, r, h); if (s == 0) continue; }
                            long likes = favoritoService.countLikes(h.getId());
                            long vistas = 0L; try { vistas = vistaRecienteRepository.countByHabitacionId(h.getId()); } catch (Exception ignored) {}
                            Map<String,Object> m = new HashMap<>();
                            m.put("residenciaId", r.getId());
                            m.put("habitacionId", h.getId());
                            if (r.getUbicacion() != null) { m.put("lat", r.getUbicacion().getLatitud()); m.put("lng", r.getUbicacion().getLongitud()); } else { m.put("lat", null); m.put("lng", null); }
                            // destacado is considered at residencia level (residencia.destacado) or habitacion.destacado
                            boolean destacado = Boolean.TRUE.equals(r.getDestacado()) || Boolean.TRUE.equals(h.getDestacado());
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
                return new ResponseEntity<>(results, HttpStatus.OK);
            } else {
                List<Map<String,Object>> results = new ArrayList<>();
                for (var r : filteredResidencias) {
                    List<edu.pe.residencias.model.entity.Habitacion> hs = habitacionRepository.findByResidenciaId(r.getId());
                    if (hs == null) hs = Collections.emptyList();
                    long likesSum = 0L; long vistasSum = 0L; int textScore = 0; boolean anyDestacado = Boolean.TRUE.equals(r.getDestacado()); java.time.LocalDateTime latestCreated = null;
                    for (var h : hs) {
                        if (h == null) continue;
                        try {
                            likesSum += favoritoService.countLikes(h.getId());
                            try { vistasSum += vistaRecienteRepository.countByHabitacionId(h.getId()); } catch (Exception ignored) {}
                            if (Boolean.TRUE.equals(h.getDestacado())) anyDestacado = true;
                            if (h.getCreatedAt() != null && (latestCreated == null || h.getCreatedAt().isAfter(latestCreated))) latestCreated = h.getCreatedAt();
                            if (q != null && !q.isBlank()) textScore += textMatchScore(q, r, h);
                        } catch (Exception ignored) {}
                    }
                    if (q != null && !q.isBlank() && textScore == 0) continue;
                    Map<String,Object> m = new HashMap<>();
                    m.put("residenciaId", r.getId());
                    if (r.getUbicacion() != null) { m.put("lat", r.getUbicacion().getLatitud()); m.put("lng", r.getUbicacion().getLongitud()); } else { m.put("lat", null); m.put("lng", null); }
                    m.put("destacado", anyDestacado); m.put("textScore", textScore); m.put("likes", likesSum); m.put("vistas", vistasSum); m.put("createdAt", latestCreated == null ? r.getCreatedAt() : latestCreated);
                    results.add(m);
                }
                results.sort(this::compareResults);
                return new ResponseEntity<>(results, HttpStatus.OK);
            }
        } catch (Exception e) {
            logger.error("Error in POST /api/search", e);
            return new ResponseEntity<>("Error interno", HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
            Object servicios = filters.get("servicios"); if (servicios != null && r.getServicios() != null) { String servFilter = servicios.toString().toLowerCase(); if (!r.getServicios().toLowerCase().contains(servFilter)) return false; }
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
                Object capacidad = filters.get("capacidad"); if (capacidad != null) { try { int c = ((Number)capacidad).intValue(); if (h.getCapacidad() == null || h.getCapacidad() < c) return false; } catch (Exception ignored) {} }
                Object piso = filters.get("piso"); if (piso != null) { try { int p = ((Number)piso).intValue(); if (h.getPiso() == null || h.getPiso() != p) return false; } catch (Exception ignored) {} }
                Object amueblado = filters.get("amueblado"); if (amueblado != null) { try { boolean a = Boolean.parseBoolean(amueblado.toString()); if (h.getAmueblado() == null || h.getAmueblado() != a) return false; } catch (Exception ignored) {} }
                Object wifi = filters.get("wifi"); if (wifi != null) { try { boolean w = Boolean.parseBoolean(wifi.toString()); if (h.getWifi() == null || h.getWifi() != w) return false; } catch (Exception ignored) {} }
                Object banoPrivado = filters.get("banoPrivado"); if (banoPrivado != null) { try { boolean b = Boolean.parseBoolean(banoPrivado.toString()); if (h.getBanoPrivado() == null || h.getBanoPrivado() != b) return false; } catch (Exception ignored) {} }
                Object permitirMascotas = filters.get("permitir_mascotas"); if (permitirMascotas != null) { try { boolean pm = Boolean.parseBoolean(permitirMascotas.toString()); if (h.getPermitir_mascotas() == null || h.getPermitir_mascotas() != pm) return false; } catch (Exception ignored) {} }
                Object estado = filters.get("estado"); if (estado != null) { String est = estado.toString().trim(); if (h.getEstado() == null || !h.getEstado().name().equalsIgnoreCase(est)) return false; }
                // Nota: 'visible' y 'destacado' no se usan como filtros aquí (visible se aplica como exclusión implícita; destacado solo para ordenamiento)
            } catch (Exception ignored) {}
        }
        try { if (precioMin != null) { if (h.getPrecioMensual() == null || h.getPrecioMensual().doubleValue() < precioMin) return false; } if (precioMax != null) { if (h.getPrecioMensual() == null || h.getPrecioMensual().doubleValue() > precioMax) return false; } } catch (Exception ignored) {}
        return true;
    }

    private int textMatchScore(String q, Residencia r, edu.pe.residencias.model.entity.Habitacion h) {
        if (q == null || q.isBlank()) return 0;
        int score = 0; String ql = q.toLowerCase();
        try { if (r != null && r.getNombre() != null && r.getNombre().toLowerCase().contains(ql)) score += 4; } catch (Exception ignored) {}
        try { if (r != null && r.getDescripcion() != null && r.getDescripcion().toLowerCase().contains(ql)) score += 2; } catch (Exception ignored) {}
        try { if (r != null && r.getServicios() != null && r.getServicios().toLowerCase().contains(ql)) score += 1; } catch (Exception ignored) {}
        // location fields: direccion, distrito, provincia, departamento, pais
        try {
            if (r != null && r.getUbicacion() != null) {
                var u = r.getUbicacion();
                try { if (u.getDireccion() != null && u.getDireccion().toLowerCase().contains(ql)) score += 3; } catch (Exception ignored) {}
                try { if (u.getDistrito() != null && u.getDistrito().toLowerCase().contains(ql)) score += 2; } catch (Exception ignored) {}
                try { if (u.getProvincia() != null && u.getProvincia().toLowerCase().contains(ql)) score += 2; } catch (Exception ignored) {}
                try { if (u.getDepartamento() != null && u.getDepartamento().toLowerCase().contains(ql)) score += 1; } catch (Exception ignored) {}
                try { if (u.getPais() != null && u.getPais().toLowerCase().contains(ql)) score += 1; } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        try { if (h != null && h.getNombre() != null && h.getNombre().toLowerCase().contains(ql)) score += 3; } catch (Exception ignored) {}
        try { if (h != null && h.getDescripcion() != null && h.getDescripcion().toLowerCase().contains(ql)) score += 1; } catch (Exception ignored) {}
        return score;
    }
}
