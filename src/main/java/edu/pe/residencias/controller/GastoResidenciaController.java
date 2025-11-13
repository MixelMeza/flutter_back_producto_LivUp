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
import edu.pe.residencias.model.entity.GastoResidencia;
import edu.pe.residencias.service.GastoResidenciaService;

@RestController
@RequestMapping("/api/gastos-residencias")
public class GastoResidenciaController {
    
    @Autowired
    private GastoResidenciaService gastoResidenciaService;

    @GetMapping
    public ResponseEntity<List<GastoResidencia>> readAll() {
        try {
            List<GastoResidencia> gastos = gastoResidenciaService.readAll();
            if (gastos.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(gastos, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<GastoResidencia> crear(@Valid @RequestBody GastoResidencia gastoResidencia) {
        try {
            GastoResidencia g = gastoResidenciaService.create(gastoResidencia);
            return new ResponseEntity<>(g, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<GastoResidencia> getGastoResidenciaId(@PathVariable("id") Long id) {
        try {
            GastoResidencia g = gastoResidenciaService.read(id).get();
            return new ResponseEntity<>(g, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GastoResidencia> delGastoResidencia(@PathVariable("id") Long id) {
        try {
            gastoResidenciaService.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateGastoResidencia(@PathVariable("id") Long id, @Valid @RequestBody GastoResidencia gastoResidencia) {
        Optional<GastoResidencia> g = gastoResidenciaService.read(id);
        if (g.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            GastoResidencia updatedGastoResidencia = gastoResidenciaService.update(gastoResidencia);
            return new ResponseEntity<>(updatedGastoResidencia, HttpStatus.OK);
        }
    }
}
