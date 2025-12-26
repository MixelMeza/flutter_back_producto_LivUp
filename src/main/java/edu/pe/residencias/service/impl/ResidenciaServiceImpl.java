package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.pe.residencias.model.entity.Residencia;
import edu.pe.residencias.model.dto.ResidenciaAdminDTO;
import edu.pe.residencias.repository.ResidenciaRepository;
import edu.pe.residencias.model.dto.MapResidenciaDTO;
import edu.pe.residencias.repository.HabitacionRepository;
import edu.pe.residencias.repository.ContratoRepository;
import edu.pe.residencias.service.ResidenciaService;
import edu.pe.residencias.repository.ImagenResidenciaRepository;
import edu.pe.residencias.service.CloudinaryService;
import edu.pe.residencias.model.entity.ImagenResidencia;

@Service
public class ResidenciaServiceImpl implements ResidenciaService {

    @Autowired
    private ResidenciaRepository repository;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private ContratoRepository contratoRepository;

    @Autowired
    private ImagenResidenciaRepository imagenResidenciaRepository;

    @Autowired
    private edu.pe.residencias.repository.ReviewRepository reviewRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Override
    public java.util.List<MapResidenciaDTO> findForMap() {
        java.util.List<Residencia> list = repository.findAllActiveWithVerifiedUsuario();
        java.util.List<MapResidenciaDTO> out = new java.util.ArrayList<>();
        for (Residencia r : list) {
            Double lat = null;
            Double lng = null;
            try {
                if (r.getUbicacion() != null) {
                    var ub = r.getUbicacion();
                    if (ub.getLatitud() != null) lat = ub.getLatitud().doubleValue();
                    if (ub.getLongitud() != null) lng = ub.getLongitud().doubleValue();
                }
            } catch (Exception ignore) {}
            if (lat == null || lng == null) continue; // skip entries without coords
            // respect visibility and estado: skip if explicitly not visible or estado is Oculto/Eliminado
            try {
                if (r.getVisible() != null && !r.getVisible()) continue;
                String estado = r.getEstado();
                if (estado != null) {
                    String norm = estado.trim().toLowerCase();
                    if (norm.equals("eliminado") || norm.equals("oculto")) continue;
                }
            } catch (Exception ignore) {}
            boolean isDest = r.getDestacado() != null ? r.getDestacado() : false;
            MapResidenciaDTO dto = new MapResidenciaDTO(r.getId(), r.getNombre() == null ? "" : r.getNombre(), lat, lng, r.getTipo() == null ? "residencia" : r.getTipo(), isDest);
            out.add(dto);
        }
        return out;
    }

    @Override
    public Residencia create(Residencia residencia) {
        if (residencia.getEstado() == null || residencia.getEstado().isEmpty()) {
            residencia.setEstado("activo");
        }
        // Todas las residencias empiezan con 0 habitaciones
        residencia.setCantidadHabitaciones(0);
        return repository.save(residencia);
    }

    @Override
    public Residencia update(Residencia residencia) {
        return repository.save(residencia);
    }

    @Override
    public void delete(Long id) {
        // First, attempt to remove files from Cloudinary related to this residencia.
        try {
            // Delete images associated to residencia
            java.util.List<ImagenResidencia> imgs = imagenResidenciaRepository.findByResidenciaId(id);
            if (imgs != null) {
                for (ImagenResidencia im : imgs) {
                    String publicId = extractPublicIdFromUrl(im.getUrl());
                    if (publicId != null && !publicId.isBlank()) {
                        try {
                            cloudinaryService.destroy(publicId, "image");
                        } catch (Exception ex) {
                            /* ignore individual failures */ }
                    }
                }
            }
            // Delete reglamento if present
            var residenciaOpt = repository.findById(id);
            if (residenciaOpt.isPresent()) {
                String reglamentoUrl = residenciaOpt.get().getReglamentoUrl();
                String reglPublicId = extractPublicIdFromUrl(reglamentoUrl);
                if (reglPublicId != null && !reglPublicId.isBlank()) {
                    try {
                        cloudinaryService.destroy(reglPublicId, "raw");
                    } catch (Exception ex) {
                        /* ignore */ }
                }
            }
        } catch (Exception e) {
            // log error if logger available; continue to delete DB records to avoid
            // orphaned references
        }

        repository.deleteById(id);
    }

    // Same public id extraction used elsewhere (best-effort)
    private String extractPublicIdFromUrl(String secureUrl) {
        if (secureUrl == null || secureUrl.isBlank())
            return null;
        int idx = secureUrl.indexOf("/upload/");
        if (idx == -1)
            return null;
        String after = secureUrl.substring(idx + "/upload/".length());
        after = after.replaceFirst("^v\\d+\\/", "");
        String noExt = after.replaceAll("\\.[^/.]+$", "");
        return noExt;
    }

    @Override
    public Optional<Residencia> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Residencia> readAll() {
        return repository.findAll();
    }

    @Override
    public Page<Residencia> findAllPaginated(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public edu.pe.residencias.model.dto.ResidenciaCardDTO getCardById(Long id) {
        java.util.Optional<Residencia> opt = repository.findById(id);
        if (opt.isEmpty()) return null;
        Residencia r = opt.get();

        // imagen principal: minimal orden (null -> treat as large)
        String imagenPrincipal = null;
        try {
            java.util.List<edu.pe.residencias.model.entity.ImagenResidencia> imgs = imagenResidenciaRepository.findByResidenciaId(id);
            if (imgs != null && !imgs.isEmpty()) {
                edu.pe.residencias.model.entity.ImagenResidencia best = null;
                for (edu.pe.residencias.model.entity.ImagenResidencia im : imgs) {
                    if (im == null) continue;
                    if (best == null) { best = im; continue; }
                    int o1 = best.getOrden() == null ? Integer.MAX_VALUE : best.getOrden();
                    int o2 = im.getOrden() == null ? Integer.MAX_VALUE : im.getOrden();
                    if (o2 < o1) best = im;
                }
                if (best != null) imagenPrincipal = best.getUrl();
            }
        } catch (Exception ignored) {}

        // rating average
        Double avg = null;
        try { avg = reviewRepository.findAveragePuntuacionByResidenciaId(id); } catch (Exception ignored) {}
        Double rating = null;
        if (avg != null) {
            // round to one decimal
            rating = Math.round(avg * 10.0) / 10.0;
        }

        // precio desde: consider only habitaciones that are visible and not ELIMINADO
        java.math.BigDecimal precioDesde = null;
        java.util.List<edu.pe.residencias.model.entity.Habitacion> filteredRooms = new java.util.ArrayList<>();
        try {
            java.util.List<edu.pe.residencias.model.entity.Habitacion> allRooms = habitacionRepository.findByResidenciaId(id);
            if (allRooms == null) allRooms = java.util.Collections.emptyList();
            for (var h : allRooms) {
                try {
                    if (h == null) continue;
                    if (h.getVisible() != null && !h.getVisible()) continue;
                    if (h.getEstado() != null && edu.pe.residencias.model.enums.HabitacionEstado.ELIMINADO.equals(h.getEstado())) continue;
                    filteredRooms.add(h);
                } catch (Exception ignore) {}
            }

            // prefer DISPONIBLE rooms for price
            java.math.BigDecimal min = null;
            for (var h : filteredRooms) {
                if (h == null || h.getPrecioMensual() == null) continue;
                if (edu.pe.residencias.model.enums.HabitacionEstado.DISPONIBLE.equals(h.getEstado())) {
                    if (min == null || h.getPrecioMensual().compareTo(min) < 0) min = h.getPrecioMensual();
                }
            }
            if (min == null) {
                for (var h : filteredRooms) {
                    if (h == null || h.getPrecioMensual() == null) continue;
                    if (min == null || h.getPrecioMensual().compareTo(min) < 0) min = h.getPrecioMensual();
                }
            }
            precioDesde = min;
        } catch (Exception ignored) {}

        // habitaciones totals and disponibles: count only filteredRooms
        int total = 0;
        int disponibles = 0;
        try {
            total = filteredRooms.size();
            for (var h : filteredRooms) {
                try {
                    if (h.getEstado() != null && edu.pe.residencias.model.enums.HabitacionEstado.DISPONIBLE.equals(h.getEstado())) disponibles++;
                } catch (Exception ignore) {}
            }
        } catch (Exception ignored) {}

        edu.pe.residencias.model.dto.ResidenciaCardDTO dto = new edu.pe.residencias.model.dto.ResidenciaCardDTO();
        dto.setId(r.getId());
        dto.setNombre(r.getNombre());
        dto.setTipo(r.getTipo());
        dto.setImagen_principal(imagenPrincipal);
        dto.setRating(rating);
        dto.setPrecio_desde(precioDesde);
        dto.setHabitaciones_disponibles(disponibles);
        dto.setHabitaciones_totales(total);
        return dto;
    }

    @Override
    public List<ResidenciaAdminDTO> mapToResidenciaAdminDTOs(List<Residencia> residencias) {
        return residencias.stream().map(residencia -> {
            ResidenciaAdminDTO dto = new ResidenciaAdminDTO();
            dto.setId(residencia.getId());
            dto.setNombre(residencia.getNombre());
            dto.setTipo(residencia.getTipo());

            // Ubicación
            if (residencia.getUbicacion() != null) {
                dto.setUbicacion(
                        residencia.getUbicacion().getDireccion() + ", " + residencia.getUbicacion().getDistrito());
            }

            // Propietario
            if (residencia.getUsuario() != null && residencia.getUsuario().getPersona() != null) {
                dto.setPropietario(residencia.getUsuario().getPersona().getNombre() + " "
                        + residencia.getUsuario().getPersona().getApellido());
            }

            dto.setCantidadHabitaciones(residencia.getCantidadHabitaciones());

            // Habitaciones ocupadas
            long ocupadas = contratoRepository.countDistinctHabitacionesByResidenciaIdAndEstado(residencia.getId(),
                    "vigente");
            dto.setHabitacionesOcupadas((int) ocupadas);

            dto.setEstado(residencia.getEstado());
            dto.setEmailContacto(residencia.getEmailContacto());
            dto.setTelefonoContacto(residencia.getTelefonoContacto());

            return dto;
        }).collect(Collectors.toList());
    }

    // Método de búsqueda de rama remota
    public Page<Residencia> search(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return repository.findAll(pageable);
        }
        String term = q.trim();
        return repository.findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCase(term, term, pageable);
    }
}
