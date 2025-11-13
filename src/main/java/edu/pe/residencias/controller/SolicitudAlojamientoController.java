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
import edu.pe.residencias.model.entity.SolicitudAlojamiento;
import edu.pe.residencias.service.SolicitudAlojamientoService;

@RestController
@RequestMapping("/api/solicitudes-alojamiento")
public class SolicitudAlojamientoController {
    
    @Autowired
    private SolicitudAlojamientoService solicitudAlojamientoService;

    @GetMapping
    public ResponseEntity<List<SolicitudAlojamiento>> readAll() {
        try {
            List<SolicitudAlojamiento> solicitudes = solicitudAlojamientoService.readAll();
            if (solicitudes.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(solicitudes, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<SolicitudAlojamiento> crear(@Valid @RequestBody SolicitudAlojamiento solicitudAlojamiento) {
        try {
            SolicitudAlojamiento s = solicitudAlojamientoService.create(solicitudAlojamiento);
            return new ResponseEntity<>(s, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitudAlojamiento> getSolicitudAlojamientoId(@PathVariable("id") Long id) {
        try {
            SolicitudAlojamiento s = solicitudAlojamientoService.read(id).get();
            return new ResponseEntity<>(s, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SolicitudAlojamiento> delSolicitudAlojamiento(@PathVariable("id") Long id) {
        try {
            solicitudAlojamientoService.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSolicitudAlojamiento(@PathVariable("id") Long id, @Valid @RequestBody SolicitudAlojamiento solicitudAlojamiento) {
        Optional<SolicitudAlojamiento> s = solicitudAlojamientoService.read(id);
        if (s.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            SolicitudAlojamiento updatedSolicitudAlojamiento = solicitudAlojamientoService.update(solicitudAlojamiento);
            return new ResponseEntity<>(updatedSolicitudAlojamiento, HttpStatus.OK);
        }
    }
}
