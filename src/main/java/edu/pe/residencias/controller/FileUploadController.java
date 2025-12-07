package edu.pe.residencias.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.pe.residencias.model.entity.ImagenHabitacion;
import edu.pe.residencias.model.entity.ImagenResidencia;
import edu.pe.residencias.model.entity.Persona;
import edu.pe.residencias.model.entity.Residencia;
import edu.pe.residencias.repository.HabitacionRepository;
import edu.pe.residencias.repository.PersonaRepository;
import edu.pe.residencias.repository.ResidenciaRepository;
import edu.pe.residencias.service.CloudinaryService;
import edu.pe.residencias.service.ImagenHabitacionService;
import edu.pe.residencias.service.ImagenResidenciaService;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private ImagenResidenciaService imagenResidenciaService;

    @Autowired
    private ImagenHabitacionService imagenHabitacionService;

    @Autowired
    private ResidenciaRepository residenciaRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private HabitacionRepository habitacionRepository;

    // Upload an image for a residencia and create ImagenResidencia record
    @PostMapping("/residencia/{residenciaId}/imagen")
    public ResponseEntity<?> uploadResidenciaImagen(@PathVariable Long residenciaId,
                                                    @RequestParam("file") MultipartFile file,
                                                    @RequestParam(value = "orden", required = false) Integer orden,
                                                    @RequestParam(value = "folder", defaultValue = "residencias") String folder) {
        try {
            var residenciaOpt = residenciaRepository.findById(residenciaId);
            if (residenciaOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Residencia no encontrada");
            Map result = cloudinaryService.uploadImage(file, folder);
            String url = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");

            ImagenResidencia img = new ImagenResidencia();
            img.setResidencia(residenciaOpt.get());
            img.setUrl(url);
            if (orden != null) {
                img.setOrden(orden);
            } else {
                // determine next orden (1-based)
                int max = 0;
                try {
                    var imgs = residenciaOpt.get().getImagenesResidencia();
                    if (imgs != null) {
                        for (var im : imgs) {
                            if (im == null || im.getOrden() == null) continue;
                            if (im.getOrden() > max) max = im.getOrden();
                        }
                    }
                } catch (Exception ignore) {}
                img.setOrden(max + 1);
            }
            ImagenResidencia created = imagenResidenciaService.create(img);

            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed: " + e.getMessage());
        }
    }

    // Upload image for habitacion
    @PostMapping("/habitacion/{habitacionId}/imagen")
    public ResponseEntity<?> uploadHabitacionImagen(@PathVariable Long habitacionId,
                                                    @RequestParam("file") MultipartFile file,
                                                    @RequestParam(value = "orden", required = false) Integer orden,
                                                    @RequestParam(value = "folder", defaultValue = "habitaciones") String folder) {
        try {
            var habitacionOpt = habitacionRepository.findById(habitacionId);
            if (habitacionOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Habitacion no encontrada");
            Map result = cloudinaryService.uploadImage(file, folder);
            String url = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");

            ImagenHabitacion img = new ImagenHabitacion();
            img.setHabitacion(habitacionOpt.get());
            img.setUrl(url);
            if (orden != null) {
                img.setOrden(orden);
            } else {
                int max = 0;
                try {
                    var imgs = habitacionOpt.get().getImagenesHabitacion();
                    if (imgs != null) {
                        for (var im : imgs) {
                            if (im == null || im.getOrden() == null) continue;
                            if (im.getOrden() > max) max = im.getOrden();
                        }
                    }
                } catch (Exception ignore) {}
                img.setOrden(max + 1);
            }
            ImagenHabitacion created = imagenHabitacionService.create(img);

            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed: " + e.getMessage());
        }
    }

    // Create ImagenHabitacion record from an existing URL (no upload)
    @PostMapping("/habitacion/{habitacionId}/imagen/url")
    public ResponseEntity<?> createHabitacionImagenFromUrl(@PathVariable Long habitacionId,
                                                           @RequestParam("url") String url,
                                                           @RequestParam(value = "orden", required = false) Integer orden) {
        try {
            var habitacionOpt = habitacionRepository.findById(habitacionId);
            if (habitacionOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Habitacion no encontrada");

            ImagenHabitacion img = new ImagenHabitacion();
            img.setHabitacion(habitacionOpt.get());
            img.setUrl(url);
            if (orden != null) {
                img.setOrden(orden);
            } else {
                int max = 0;
                try {
                    var imgs = habitacionOpt.get().getImagenesHabitacion();
                    if (imgs != null) {
                        for (var im : imgs) {
                            if (im == null || im.getOrden() == null) continue;
                            if (im.getOrden() > max) max = im.getOrden();
                        }
                    }
                } catch (Exception ignore) {}
                img.setOrden(max + 1);
            }
            ImagenHabitacion created = imagenHabitacionService.create(img);

            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Create failed: " + e.getMessage());
        }
    }

    // Upload persona photo and update persona.fotoUrl
    @PostMapping("/persona/{personaId}/foto")
    public ResponseEntity<?> uploadPersonaFoto(@PathVariable Long personaId,
                                               @RequestParam("file") MultipartFile file,
                                               @RequestParam(value = "folder", defaultValue = "personas") String folder) {
        try {
            var personaOpt = personaRepository.findById(personaId);
            if (personaOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Persona no encontrada");
            Map result = cloudinaryService.uploadImage(file, folder);
            String url = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");

            Persona p = personaOpt.get();
            p.setFotoUrl(url);
            personaRepository.save(p);

            Map<String, Object> resp = new HashMap<>();
            resp.put("url", url);
            resp.put("result", result);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed: " + e.getMessage());
        }
    }

    // Upload residencia reglamento (pdf/raw) and update residencia.reglamentoUrl
    @PostMapping("/residencia/{residenciaId}/reglamento")
    public ResponseEntity<?> uploadResidenciaReglamento(@PathVariable Long residenciaId,
                                                         @RequestParam("file") MultipartFile file,
                                                         @RequestParam(value = "folder", defaultValue = "reglamentos") String folder) {
        try {
            var residenciaOpt = residenciaRepository.findById(residenciaId);
            if (residenciaOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Residencia no encontrada");
            Map result = cloudinaryService.uploadRaw(file, folder);
            String url = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");

            Residencia r = residenciaOpt.get();
            r.setReglamentoUrl(url);
            residenciaRepository.save(r);

            Map<String, Object> resp = new HashMap<>();
            resp.put("url", url);
            resp.put("result", result);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed: " + e.getMessage());
        }
    }

    // Delete imagen residencia by imagen id (removes from Cloudinary and DB)
    @DeleteMapping("/imagen-residencia/{imagenId}")
    public ResponseEntity<?> deleteImagenResidencia(@PathVariable Long imagenId) {
        try {
            var imgOpt = imagenResidenciaService.read(imagenId);
            if (imgOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Imagen no encontrada");
            var img = imgOpt.get();
            String publicId = extractPublicIdFromUrl(img.getUrl());
            if (publicId != null && !publicId.isBlank()) {
                cloudinaryService.destroy(publicId, "image");
            }
            imagenResidenciaService.delete(imagenId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Delete failed: " + e.getMessage());
        }
    }

    // Delete imagen habitacion
    @DeleteMapping("/imagen-habitacion/{imagenId}")
    public ResponseEntity<?> deleteImagenHabitacion(@PathVariable Long imagenId) {
        try {
            var imgOpt = imagenHabitacionService.read(imagenId);
            if (imgOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Imagen no encontrada");
            var img = imgOpt.get();
            String publicId = extractPublicIdFromUrl(img.getUrl());
            if (publicId != null && !publicId.isBlank()) {
                cloudinaryService.destroy(publicId, "image");
            }
            imagenHabitacionService.delete(imagenId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Delete failed: " + e.getMessage());
        }
    }

    // Delete persona photo
    @DeleteMapping("/persona/{personaId}/foto")
    public ResponseEntity<?> deletePersonaFoto(@PathVariable Long personaId) {
        try {
            var personaOpt = personaRepository.findById(personaId);
            if (personaOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Persona no encontrada");
            var p = personaOpt.get();
            String publicId = extractPublicIdFromUrl(p.getFotoUrl());
            if (publicId != null && !publicId.isBlank()) {
                cloudinaryService.destroy(publicId, "image");
            }
            p.setFotoUrl(null);
            personaRepository.save(p);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Delete failed: " + e.getMessage());
        }
    }

    // Delete residencia reglamento (pdf/raw)
    @DeleteMapping("/residencia/{residenciaId}/reglamento")
    public ResponseEntity<?> deleteResidenciaReglamento(@PathVariable Long residenciaId) {
        try {
            var residenciaOpt = residenciaRepository.findById(residenciaId);
            if (residenciaOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Residencia no encontrada");
            var r = residenciaOpt.get();
            String publicId = extractPublicIdFromUrl(r.getReglamentoUrl());
            if (publicId != null && !publicId.isBlank()) {
                cloudinaryService.destroy(publicId, "raw");
            }
            r.setReglamentoUrl(null);
            residenciaRepository.save(r);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Delete failed: " + e.getMessage());
        }
    }

    // Extract Cloudinary public_id from a secure_url (best-effort). Returns null if can't parse.
    private String extractPublicIdFromUrl(String secureUrl) {
        if (secureUrl == null || secureUrl.isBlank()) return null;
        int idx = secureUrl.indexOf("/upload/");
        if (idx == -1) return null;
        String after = secureUrl.substring(idx + "/upload/".length());
            // remove version prefix if present: v123456789/
            after = after.replaceFirst("^v\\d+/", "");
        // remove extension
        String noExt = after.replaceAll("\\.[^/.]+$", "");
        return noExt;
    }
}
