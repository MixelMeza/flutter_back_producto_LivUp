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
import edu.pe.residencias.model.entity.Residencia;
import edu.pe.residencias.service.ResidenciaService;

@RestController
@RequestMapping("/api/residencias")
public class ResidenciaController {
    
    @Autowired
    private ResidenciaService residenciaService;

    @GetMapping
    public ResponseEntity<List<Residencia>> readAll() {
        try {
            List<Residencia> residencias = residenciaService.readAll();
            if (residencias.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(residencias, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<Residencia> crear(@Valid @RequestBody Residencia residencia) {
        try {
            Residencia r = residenciaService.create(residencia);
            return new ResponseEntity<>(r, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Residencia> getResidenciaId(@PathVariable("id") Long id) {
        try {
            Residencia r = residenciaService.read(id).get();
            return new ResponseEntity<>(r, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Residencia> delResidencia(@PathVariable("id") Long id) {
        try {
            residenciaService.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateResidencia(@PathVariable("id") Long id, @Valid @RequestBody Residencia residencia) {
        Optional<Residencia> r = residenciaService.read(id);
        if (r.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            Residencia updatedResidencia = residenciaService.update(residencia);
            return new ResponseEntity<>(updatedResidencia, HttpStatus.OK);
        }
    }
}
