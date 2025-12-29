package edu.pe.residencias.service.impl;

import edu.pe.residencias.model.entity.analytics.FavoritoHabitacionSemana;
import edu.pe.residencias.model.entity.analytics.VistaHabitacionSemana;
import edu.pe.residencias.repository.FavoritoHabitacionSemanaRepository;
import edu.pe.residencias.repository.HabitacionRepository;
import edu.pe.residencias.repository.VistaHabitacionSemanaRepository;
import edu.pe.residencias.service.HabitacionStatsSemanaService;
import edu.pe.residencias.util.WeekKeyUtil;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HabitacionStatsSemanaServiceImpl implements HabitacionStatsSemanaService {

    @Autowired
    private VistaHabitacionSemanaRepository vistaSemanaRepository;

    @Autowired
    private FavoritoHabitacionSemanaRepository favoritoSemanaRepository;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Override
    @Transactional
    public void incrementarVista(Long habitacionId) {
        if (habitacionId == null) return;
        if (!habitacionRepository.existsById(habitacionId)) return;

        String weekKey = WeekKeyUtil.currentWeekKeyLima();

        int updated = vistaSemanaRepository.increment(habitacionId, weekKey);
        if (updated > 0) return;

        VistaHabitacionSemana row = new VistaHabitacionSemana();
        row.setHabitacion(habitacionRepository.getReferenceById(habitacionId));
        row.setWeekKey(weekKey);
        row.setTotalVistas(1L);

        try {
            vistaSemanaRepository.saveAndFlush(row);
        } catch (DataIntegrityViolationException ex) {
            // Race: another request inserted the row first; retry increment.
            vistaSemanaRepository.increment(habitacionId, weekKey);
        }
    }

    @Override
    @Transactional
    public void incrementarFavorito(Long habitacionId) {
        if (habitacionId == null) return;
        if (!habitacionRepository.existsById(habitacionId)) return;

        String weekKey = WeekKeyUtil.currentWeekKeyLima();

        int updated = favoritoSemanaRepository.increment(habitacionId, weekKey);
        if (updated > 0) return;

        FavoritoHabitacionSemana row = new FavoritoHabitacionSemana();
        row.setHabitacion(habitacionRepository.getReferenceById(habitacionId));
        row.setWeekKey(weekKey);
        row.setTotalFavoritos(1L);

        try {
            favoritoSemanaRepository.saveAndFlush(row);
        } catch (DataIntegrityViolationException ex) {
            favoritoSemanaRepository.increment(habitacionId, weekKey);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> masVistasSemana(int limit) {
        int safeLimit = limit <= 0 ? 5 : limit;
        String weekKey = WeekKeyUtil.currentWeekKeyLima();
        return vistaSemanaRepository.findTopHabitacionIds(weekKey, PageRequest.of(0, safeLimit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> masLikeadasSemana(int limit) {
        int safeLimit = limit <= 0 ? 5 : limit;
        String weekKey = WeekKeyUtil.currentWeekKeyLima();
        return favoritoSemanaRepository.findTopHabitacionIds(weekKey, PageRequest.of(0, safeLimit));
    }
}
