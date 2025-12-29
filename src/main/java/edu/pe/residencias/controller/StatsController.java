package edu.pe.residencias.controller;

import edu.pe.residencias.service.HabitacionStatsSemanaService;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats/habitaciones")
public class StatsController {

    @Autowired
    private HabitacionStatsSemanaService statsSemanaService;

    @GetMapping("/mas-vistas-semana")
    public List<Long> masVistasSemana(@RequestParam(name = "limit", defaultValue = "5") int limit) {
        return statsSemanaService.masVistasSemana(limit);
    }

    @GetMapping("/mas-likeadas-semana")
    public List<Long> masLikeadasSemana(@RequestParam(name = "limit", defaultValue = "5") int limit) {
        return statsSemanaService.masLikeadasSemana(limit);
    }
}
