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
import edu.pe.residencias.model.entity.ImagenHabitacion;
import edu.pe.residencias.repository.ImagenHabitacionRepository;
import edu.pe.residencias.service.CloudinaryService;
import edu.pe.residencias.service.ImagenHabitacionService;

@RestController
@RequestMapping("/api/imagenes-habitacion")
public class ImagenHabitacionController {

    @Autowired
    private ImagenHabitacionService imagenHabitacionService;

    @Autowired
    private ImagenHabitacionRepository imagenHabitacionRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @GetMapping
    public ResponseEntity<List<ImagenHabitacion>> readAll() {
        try {
            List<ImagenHabitacion> imagenes = imagenHabitacionService.readAll();
            if (imagenes.isEmpty())
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            return new ResponseEntity<>(imagenes, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/habitacion/{habitacionId}")
    public ResponseEntity<?> listByHabitacion(@PathVariable Long habitacionId) {
        try {
            List<ImagenHabitacion> list = imagenHabitacionRepository.findByHabitacionId(habitacionId);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<ImagenHabitacion> crear(@Valid @RequestBody ImagenHabitacion imagenHabitacion) {
        try {
            ImagenHabitacion i = imagenHabitacionService.create(imagenHabitacion);
            return new ResponseEntity<>(i, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getImagenHabitacionId(@PathVariable("id") Long id) {
        try {
            Optional<ImagenHabitacion> opt = imagenHabitacionService.read(id);
            if (opt.isEmpty())
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            return new ResponseEntity<>(opt.get(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateImagenHabitacion(@PathVariable("id") Long id,
            @Valid @RequestBody ImagenHabitacion imagenHabitacion) {
        Optional<ImagenHabitacion> i = imagenHabitacionService.read(id);
        if (i.isEmpty())
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        try {
            ImagenHabitacion existing = i.get();
            existing.setOrden(imagenHabitacion.getOrden());
            existing.setEstado(imagenHabitacion.getEstado());
            ImagenHabitacion updatedImagenHabitacion = imagenHabitacionService.update(existing);
            return new ResponseEntity<>(updatedImagenHabitacion, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delImagenHabitacion(@PathVariable("id") Long id) {
        try {
            Optional<ImagenHabitacion> opt = imagenHabitacionService.read(id);
            if (opt.isEmpty())
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            ImagenHabitacion img = opt.get();
            String publicId = extractPublicIdFromUrl(img.getUrl());
            if (publicId != null && !publicId.isBlank()) {
                cloudinaryService.destroy(publicId, "image");
            }
            imagenHabitacionService.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Extract Cloudinary public_id from a secure_url (best-effort). Returns null if
    // can't parse.
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
}