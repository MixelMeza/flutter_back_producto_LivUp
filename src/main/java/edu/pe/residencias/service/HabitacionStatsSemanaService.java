package edu.pe.residencias.service;

import java.util.List;

public interface HabitacionStatsSemanaService {

    void incrementarVista(Long habitacionId);

    void incrementarFavorito(Long habitacionId);

    List<Long> masVistasSemana(int limit);

    List<Long> masLikeadasSemana(int limit);
}
