package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.ImagenHabitacion;
import edu.pe.residencias.repository.ImagenHabitacionRepository;
import edu.pe.residencias.service.ImagenHabitacionService;
import org.springframework.transaction.annotation.Transactional;
import edu.pe.residencias.repository.HabitacionRepository;
import edu.pe.residencias.model.entity.Habitacion;

@Service
public class ImagenHabitacionServiceImpl implements ImagenHabitacionService {

    @Autowired
    private ImagenHabitacionRepository repository;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Override
    public ImagenHabitacion create(ImagenHabitacion imagenHabitacion) {
        return repository.save(imagenHabitacion);
    }

    @Override
    public ImagenHabitacion update(ImagenHabitacion imagenHabitacion) {
        return repository.save(imagenHabitacion);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<ImagenHabitacion> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<ImagenHabitacion> readAll() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public List<ImagenHabitacion> updateListForHabitacion(Long habitacionId, java.util.List<String> urls) {
        if (habitacionId == null) throw new IllegalArgumentException("habitacionId es requerido");
        var opt = habitacionRepository.findById(habitacionId);
        if (opt.isEmpty()) throw new IllegalArgumentException("Habitacion no encontrada");
        Habitacion habitacion = opt.get();

        List<ImagenHabitacion> existing = repository.findByHabitacionId(habitacionId);
        java.util.Map<String, ImagenHabitacion> mapByUrl = new java.util.HashMap<>();
        for (ImagenHabitacion im : existing) {
            if (im.getUrl() != null) mapByUrl.put(im.getUrl(), im);
        }

        java.util.Set<String> incomingSet = new java.util.HashSet<>();
        java.util.List<ImagenHabitacion> result = new java.util.ArrayList<>();

        int index = 1; // use 1-based ordering
        for (String url : urls == null ? java.util.List.<String>of() : urls) {
            incomingSet.add(url);
            ImagenHabitacion im = mapByUrl.get(url);
            if (im != null) {
                if (im.getOrden() == null || im.getOrden() != index) {
                    im.setOrden(index);
                    repository.save(im);
                }
            } else {
                ImagenHabitacion nuevo = new ImagenHabitacion();
                nuevo.setHabitacion(habitacion);
                nuevo.setUrl(url);
                nuevo.setOrden(index);
                nuevo = repository.save(nuevo);
                im = nuevo;
            }
            result.add(im);
            index++;
        }

        for (ImagenHabitacion im : existing) {
            if (im.getUrl() == null) continue;
            if (!incomingSet.contains(im.getUrl())) {
                try { repository.delete(im); } catch (Exception ignore) {}
            }
        }

        return result;
    }
}
