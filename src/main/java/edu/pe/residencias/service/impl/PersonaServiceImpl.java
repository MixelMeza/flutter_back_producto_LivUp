package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.Persona;
import edu.pe.residencias.repository.PersonaRepository;
import edu.pe.residencias.service.PersonaService;

@Service
public class PersonaServiceImpl implements PersonaService {

    @Autowired
    private PersonaRepository repository;

    @Autowired
    private edu.pe.residencias.service.ResidenciaService residenciaService;

    @Autowired
    private edu.pe.residencias.service.UsuarioService usuarioService;

    @Autowired
    private edu.pe.residencias.service.CloudinaryService cloudinaryService;

    @Override
    public Persona create(Persona persona) {
        return repository.save(persona);
    }

    @Override
    public Persona update(Persona persona) {
        return repository.save(persona);
    }

    @Override
    public void delete(Long id) {
        // Load persona to clean up related files (foto and nested residencias)
        try {
            var opt = repository.findById(id);
            if (opt.isPresent()) {
                var persona = opt.get();
                // delete persona photo from Cloudinary
                String publicId = extractPublicIdFromUrl(persona.getFotoUrl());
                if (publicId != null && !publicId.isBlank()) {
                    try { cloudinaryService.destroy(publicId, "image"); } catch (Exception ex) { }
                }
                // For each usuario of this persona, delete their residencias via residenciaService
                if (persona.getUsuarios() != null) {
                    for (var u : persona.getUsuarios()) {
                        if (u.getResidencias() != null) {
                            for (var r : u.getResidencias()) {
                                try { residenciaService.delete(r.getId()); } catch (Exception ex) { }
                            }
                        }
                        // Optionally delete usuario itself; we'll rely on repository.delete to cascade
                        try { usuarioService.delete(u.getId()); } catch (Exception ex) { }
                    }
                }
            }
        } catch (Exception e) {
            // ignore and continue to delete DB record
        }

        repository.deleteById(id);
    }

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
    public Optional<Persona> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Persona> readAll() {
        return repository.findAll();
    }
}
