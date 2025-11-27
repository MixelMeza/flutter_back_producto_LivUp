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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import edu.pe.residencias.model.entity.Habitacion;
import edu.pe.residencias.model.entity.Residencia;
import edu.pe.residencias.service.HabitacionService;
import edu.pe.residencias.repository.ResidenciaRepository;

@RestController
@RequestMapping("/api/habitaciones")
public class HabitacionController {
    
    @Autowired
    private HabitacionService habitacionService;
    
    @Autowired
    private edu.pe.residencias.repository.HabitacionRepository habitacionRepository;
    
    @Autowired
    private ResidenciaRepository residenciaRepository;

    @GetMapping
    public ResponseEntity<List<Habitacion>> readAll() {
        try {
            List<Habitacion> habitaciones = habitacionService.readAll();
            if (habitaciones.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(habitaciones, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<Habitacion> crear(@Valid @RequestBody Habitacion habitacion) {
        try {
            Habitacion h = habitacionService.create(habitacion);
            return new ResponseEntity<>(h, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Habitacion> getHabitacionId(@PathVariable("id") Long id) {
        try {
            Habitacion h = habitacionService.read(id).get();
            return new ResponseEntity<>(h, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Habitacion> delHabitacion(@PathVariable("id") Long id) {
        try {
            habitacionService.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateHabitacion(@PathVariable("id") Long id, @Valid @RequestBody Habitacion habitacion) {
        Optional<Habitacion> h = habitacionService.read(id);
        if (h.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            Habitacion updatedHabitacion = habitacionService.update(habitacion);
            return new ResponseEntity<>(updatedHabitacion, HttpStatus.OK);
        }
    }
    
    @GetMapping("/residencia/{residenciaId}/disponibles")
    public ResponseEntity<?> getHabitacionesDisponibles(@PathVariable("residenciaId") Long residenciaId) {
        try {
            // Verificar si la residencia existe
            Optional<Residencia> residencia = residenciaRepository.findById(residenciaId);
            if (residencia.isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("No existe la residencia con ID: " + residenciaId);
            }
            
            // Buscar habitaciones disponibles
            List<Habitacion> habitaciones = habitacionRepository.findByResidenciaIdAndEstado(residenciaId, "disponible");
            
            if (habitaciones.isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("La residencia no tiene habitaciones disponibles");
            }
            
            return new ResponseEntity<>(habitaciones, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al buscar habitaciones: " + e.getMessage());
        }
    }
}
