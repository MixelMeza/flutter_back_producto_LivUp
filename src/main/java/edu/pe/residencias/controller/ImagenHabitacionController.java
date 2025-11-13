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
import edu.pe.residencias.service.ImagenHabitacionService;

@RestController
@RequestMapping("/api/imagenes-habitaciones")
public class ImagenHabitacionController {
    
    @Autowired
    private ImagenHabitacionService imagenHabitacionService;

    @GetMapping
    public ResponseEntity<List<ImagenHabitacion>> readAll() {
        try {
            List<ImagenHabitacion> imagenes = imagenHabitacionService.readAll();
            if (imagenes.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(imagenes, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
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
    public ResponseEntity<ImagenHabitacion> getImagenHabitacionId(@PathVariable("id") Long id) {
        try {
            ImagenHabitacion i = imagenHabitacionService.read(id).get();
            return new ResponseEntity<>(i, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ImagenHabitacion> delImagenHabitacion(@PathVariable("id") Long id) {
        try {
            imagenHabitacionService.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateImagenHabitacion(@PathVariable("id") Long id, @Valid @RequestBody ImagenHabitacion imagenHabitacion) {
        Optional<ImagenHabitacion> i = imagenHabitacionService.read(id);
        if (i.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            ImagenHabitacion updatedImagenHabitacion = imagenHabitacionService.update(imagenHabitacion);
            return new ResponseEntity<>(updatedImagenHabitacion, HttpStatus.OK);
        }
    }
}
