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
import edu.pe.residencias.model.entity.Contrato;
import edu.pe.residencias.service.ContratoService;

@RestController
@RequestMapping("/api/contratos")
public class ContratoController {
    
    @Autowired
    private ContratoService contratoService;

    @GetMapping
    public ResponseEntity<List<Contrato>> readAll() {
        try {
            List<Contrato> contratos = contratoService.readAll();
            if (contratos.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(contratos, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<Contrato> crear(@Valid @RequestBody Contrato contrato) {
        try {
            Contrato c = contratoService.create(contrato);
            return new ResponseEntity<>(c, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contrato> getContratoId(@PathVariable("id") Long id) {
        try {
            Contrato c = contratoService.read(id).get();
            return new ResponseEntity<>(c, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Contrato> delContrato(@PathVariable("id") Long id) {
        try {
            contratoService.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateContrato(@PathVariable("id") Long id, @Valid @RequestBody Contrato contrato) {
        Optional<Contrato> c = contratoService.read(id);
        if (c.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            Contrato updatedContrato = contratoService.update(contrato);
            return new ResponseEntity<>(updatedContrato, HttpStatus.OK);
        }
    }
}
