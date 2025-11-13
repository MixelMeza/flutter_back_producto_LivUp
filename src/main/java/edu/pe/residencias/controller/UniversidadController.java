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
import edu.pe.residencias.model.entity.Universidad;
import edu.pe.residencias.service.UniversidadService;

@RestController
@RequestMapping("/api/universidades")
public class UniversidadController {
    
    @Autowired
    private UniversidadService universidadService;

    @GetMapping
    public ResponseEntity<List<Universidad>> readAll() {
        try {
            List<Universidad> universidades = universidadService.readAll();
            if (universidades.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(universidades, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<Universidad> crear(@Valid @RequestBody Universidad universidad) {
        try {
            Universidad u = universidadService.create(universidad);
            return new ResponseEntity<>(u, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Universidad> getUniversidadId(@PathVariable("id") Long id) {
        try {
            Universidad u = universidadService.read(id).get();
            return new ResponseEntity<>(u, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Universidad> delUniversidad(@PathVariable("id") Long id) {
        try {
            universidadService.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUniversidad(@PathVariable("id") Long id, @Valid @RequestBody Universidad universidad) {
        Optional<Universidad> u = universidadService.read(id);
        if (u.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            Universidad updatedUniversidad = universidadService.update(universidad);
            return new ResponseEntity<>(updatedUniversidad, HttpStatus.OK);
        }
    }
}
