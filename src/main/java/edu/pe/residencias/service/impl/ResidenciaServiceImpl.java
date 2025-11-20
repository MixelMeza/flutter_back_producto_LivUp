package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.Residencia;
import edu.pe.residencias.repository.ResidenciaRepository;
import edu.pe.residencias.service.ResidenciaService;
import edu.pe.residencias.repository.ImagenResidenciaRepository;
import edu.pe.residencias.service.CloudinaryService;
import edu.pe.residencias.model.entity.ImagenResidencia;

@Service
public class ResidenciaServiceImpl implements ResidenciaService {

    @Autowired
    private ResidenciaRepository repository;

    @Autowired
    private ImagenResidenciaRepository imagenResidenciaRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Override
    public Residencia create(Residencia residencia) {
        if (residencia.getEstado() == null || residencia.getEstado().isEmpty()) {
            residencia.setEstado("activo");
        }
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
                        try { cloudinaryService.destroy(publicId, "image"); } catch (Exception ex) { /* ignore individual failures */ }
                    }
                }
            }
            // Delete reglamento if present
            var residenciaOpt = repository.findById(id);
            if (residenciaOpt.isPresent()) {
                String reglamentoUrl = residenciaOpt.get().getReglamentoUrl();
                String reglPublicId = extractPublicIdFromUrl(reglamentoUrl);
                if (reglPublicId != null && !reglPublicId.isBlank()) {
                    try { cloudinaryService.destroy(reglPublicId, "raw"); } catch (Exception ex) { /* ignore */ }
                }
            }
        } catch (Exception e) {
            // log error if logger available; continue to delete DB records to avoid orphaned references
        }

        repository.deleteById(id);
    }

    // Same public id extraction used elsewhere (best-effort)
    private String extractPublicIdFromUrl(String secureUrl) {
        if (secureUrl == null || secureUrl.isBlank()) return null;
        int idx = secureUrl.indexOf("/upload/");
        if (idx == -1) return null;
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
    public Page<Residencia> search(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return repository.findAll(pageable);
        }
        String term = q.trim();
        return repository.findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCase(term, term, pageable);
    }
}
