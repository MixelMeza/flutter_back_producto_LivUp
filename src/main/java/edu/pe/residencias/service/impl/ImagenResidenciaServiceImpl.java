package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.ImagenResidencia;
import edu.pe.residencias.repository.ImagenResidenciaRepository;
import edu.pe.residencias.service.ImagenResidenciaService;
import org.springframework.transaction.annotation.Transactional;
import edu.pe.residencias.repository.ResidenciaRepository;
import edu.pe.residencias.model.entity.Residencia;

@Service
public class ImagenResidenciaServiceImpl implements ImagenResidenciaService {

    @Autowired
    private ImagenResidenciaRepository repository;

    @Autowired
    private ResidenciaRepository residenciaRepository;

    @Override
    public ImagenResidencia create(ImagenResidencia imagenResidencia) {
        return repository.save(imagenResidencia);
    }

    @Override
    public ImagenResidencia update(ImagenResidencia imagenResidencia) {
        return repository.save(imagenResidencia);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<ImagenResidencia> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<ImagenResidencia> readAll() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public List<ImagenResidencia> updateListForResidencia(Long residenciaId, java.util.List<String> urls) {
        // ensure residencia exists
        Residencia residencia = null;
        if (residenciaId != null) {
            var opt = residenciaRepository.findById(residenciaId);
            if (opt.isEmpty()) throw new IllegalArgumentException("Residencia no encontrada");
            residencia = opt.get();
        } else {
            throw new IllegalArgumentException("residenciaId es requerido");
        }

        List<ImagenResidencia> existing = repository.findByResidenciaId(residenciaId);
        java.util.Map<String, ImagenResidencia> mapByUrl = new java.util.HashMap<>();
        for (ImagenResidencia im : existing) {
            if (im.getUrl() != null) mapByUrl.put(im.getUrl(), im);
        }

        java.util.Set<String> incomingSet = new java.util.HashSet<>();
        java.util.List<ImagenResidencia> result = new java.util.ArrayList<>();

        int index = 0;
        for (String url : urls == null ? java.util.List.<String>of() : urls) {
            incomingSet.add(url);
            ImagenResidencia im = mapByUrl.get(url);
            if (im != null) {
                // update order if needed
                if (im.getOrden() == null || im.getOrden() != index) {
                    im.setOrden(index);
                    repository.save(im);
                }
            } else {
                // create new
                ImagenResidencia nuevo = new ImagenResidencia();
                nuevo.setResidencia(residencia);
                nuevo.setUrl(url);
                nuevo.setOrden(index);
                nuevo = repository.save(nuevo);
                im = nuevo;
            }
            result.add(im);
            index++;
        }

        // delete images that are not in incoming list
        for (ImagenResidencia im : existing) {
            if (im.getUrl() == null) continue;
            if (!incomingSet.contains(im.getUrl())) {
                try {
                    repository.delete(im);
                } catch (Exception ignore) {}
            }
        }

        return result;
    }
}
