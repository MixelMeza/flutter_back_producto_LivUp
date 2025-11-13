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
import edu.pe.residencias.model.entity.ImagenResidencia;
import edu.pe.residencias.service.ImagenResidenciaService;

@RestController
@RequestMapping("/api/imagenes-residencias")
public class ImagenResidenciaController {
    
    @Autowired
    private ImagenResidenciaService imagenResidenciaService;

    @GetMapping
    public ResponseEntity<List<ImagenResidencia>> readAll() {
        try {
            List<ImagenResidencia> imagenes = imagenResidenciaService.readAll();
            if (imagenes.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(imagenes, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<ImagenResidencia> crear(@Valid @RequestBody ImagenResidencia imagenResidencia) {
        try {
            ImagenResidencia i = imagenResidenciaService.create(imagenResidencia);
            return new ResponseEntity<>(i, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImagenResidencia> getImagenResidenciaId(@PathVariable("id") Long id) {
        try {
            ImagenResidencia i = imagenResidenciaService.read(id).get();
            return new ResponseEntity<>(i, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ImagenResidencia> delImagenResidencia(@PathVariable("id") Long id) {
        try {
            imagenResidenciaService.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateImagenResidencia(@PathVariable("id") Long id, @Valid @RequestBody ImagenResidencia imagenResidencia) {
        Optional<ImagenResidencia> i = imagenResidenciaService.read(id);
        if (i.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            ImagenResidencia updatedImagenResidencia = imagenResidenciaService.update(imagenResidencia);
            return new ResponseEntity<>(updatedImagenResidencia, HttpStatus.OK);
        }
    }
}
