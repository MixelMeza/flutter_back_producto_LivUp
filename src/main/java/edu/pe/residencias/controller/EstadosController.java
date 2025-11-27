package edu.pe.residencias.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pe.residencias.model.dto.EstadosReferenciaDTO;

@RestController
@RequestMapping("/api/estados")
public class EstadosController {
    
    @GetMapping("/referencia")
    public ResponseEntity<EstadosReferenciaDTO> getEstadosReferencia() {
        try {
            EstadosReferenciaDTO estados = EstadosReferenciaDTO.crear();
            return new ResponseEntity<>(estados, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
