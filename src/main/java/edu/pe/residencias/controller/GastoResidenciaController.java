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
import java.util.Map;
import edu.pe.residencias.model.entity.GastoResidencia;
import edu.pe.residencias.model.dto.GastoResidenciaCreateDTO;
import edu.pe.residencias.repository.ResidenciaRepository;
import java.time.LocalDate;
import java.math.BigDecimal;
import edu.pe.residencias.service.GastoResidenciaService;

@RestController
@RequestMapping("/api/gastos-residencias")
public class GastoResidenciaController {
    
    @Autowired
    private GastoResidenciaService gastoResidenciaService;

    @Autowired
    private ResidenciaRepository residenciaRepository;

    @GetMapping
    public ResponseEntity<List<GastoResidencia>> readAll(
            @org.springframework.web.bind.annotation.RequestParam(value = "residenciaId", required = false) Long residenciaId,
            @org.springframework.web.bind.annotation.RequestParam(value = "periodo", required = false) String periodo,
            @org.springframework.web.bind.annotation.RequestParam(value = "estadoPago", required = false) String estadoPago,
            @org.springframework.web.bind.annotation.RequestParam(value = "tipoGasto", required = false) String tipoGasto,
            @org.springframework.web.bind.annotation.RequestParam(value = "metodoPago", required = false) String metodoPago
    ) {
        try {
            List<GastoResidencia> gastos = gastoResidenciaService.readAll();

            // start with residencia filter if provided to reduce data set
            if (residenciaId != null) {
                gastos = repository.findByResidenciaId(residenciaId);
            }

            // apply other optional filters in-memory
            java.util.stream.Stream<GastoResidencia> stream = gastos.stream();

            if (periodo != null && !periodo.isBlank()) {
                stream = stream.filter(g -> periodo.equals(g.getPeriodo()));
            }

            if (estadoPago != null && !estadoPago.isBlank()) {
                try {
                    GastoResidencia.EstadoPago ep = GastoResidencia.EstadoPago.valueOf(estadoPago.toUpperCase());
                    stream = stream.filter(g -> ep.equals(g.getEstadoPago()));
                } catch (IllegalArgumentException ex) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.List.of());
                }
            }

            if (tipoGasto != null && !tipoGasto.isBlank()) {
                try {
                    GastoResidencia.TipoGasto tg = GastoResidencia.TipoGasto.valueOf(tipoGasto.toUpperCase());
                    stream = stream.filter(g -> tg.equals(g.getTipoGasto()));
                } catch (IllegalArgumentException ex) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.List.of());
                }
            }

            if (metodoPago != null && !metodoPago.isBlank()) {
                try {
                    GastoResidencia.MetodoPago mp = GastoResidencia.MetodoPago.valueOf(metodoPago.toUpperCase());
                    stream = stream.filter(g -> mp.equals(g.getMetodoPago()));
                } catch (IllegalArgumentException ex) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.List.of());
                }
            }

            List<GastoResidencia> out = stream.collect(java.util.stream.Collectors.toList());
            if (out.isEmpty()) return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            return new ResponseEntity<>(out, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody GastoResidenciaCreateDTO dto) {
        try {
            // residencia required
            var residenciaOpt = residenciaRepository.findById(dto.getResidenciaId());
            if (residenciaOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "residenciaId not found"));
            }

            GastoResidencia g = new GastoResidencia();
            g.setResidencia(residenciaOpt.get());
            g.setConcepto(dto.getConcepto());
            g.setDescripcion(dto.getDescripcion());

            // tipoGasto default OTRO
            if (dto.getTipoGasto() == null || dto.getTipoGasto().isBlank()) {
                g.setTipoGasto(GastoResidencia.TipoGasto.OTRO);
            } else {
                try {
                    g.setTipoGasto(GastoResidencia.TipoGasto.valueOf(dto.getTipoGasto().toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "invalid tipoGasto", "allowed", GastoResidencia.TipoGasto.values()));
                }
            }

            g.setPeriodo(dto.getPeriodo());

            // parse fechaGasto if provided
            if (dto.getFechaGasto() != null && !dto.getFechaGasto().isBlank()) {
                try {
                    g.setFechaGasto(LocalDate.parse(dto.getFechaGasto()));
                } catch (Exception ex) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "invalid fechaGasto format, expected yyyy-MM-dd"));
                }
            }

            g.setMonto(dto.getMonto());

            // estadoPago default PENDIENTE (ignore client value)
            g.setEstadoPago(GastoResidencia.EstadoPago.PENDIENTE);

            // metodoPago is required
            if (dto.getMetodoPago() == null || dto.getMetodoPago().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "metodoPago is required", "allowed", GastoResidencia.MetodoPago.values()));
            }
            try {
                g.setMetodoPago(GastoResidencia.MetodoPago.valueOf(dto.getMetodoPago().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "invalid metodoPago", "allowed", GastoResidencia.MetodoPago.values()));
            }

            g.setComprobanteUrl(dto.getComprobanteUrl());

            GastoResidencia created = gastoResidenciaService.create(g);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
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
