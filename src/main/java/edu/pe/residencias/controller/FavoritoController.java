package edu.pe.residencias.controller;

import edu.pe.residencias.model.entity.Favorito;
import edu.pe.residencias.service.FavoritoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/favoritos")
public class FavoritoController {

    @Autowired
    private FavoritoService favoritoService;

    @GetMapping
    public List<Favorito> listarFavoritos() {
        return favoritoService.findAll();
    }

    @GetMapping("/{id}")
    public Favorito getFavoritoById(@PathVariable Long id) {
        return favoritoService.findById(id);
    }

    @PostMapping
    public Favorito crearFavorito(@RequestBody Favorito favorito) {
        return favoritoService.save(favorito);
    }

    @PutMapping("/{id}")
    public Favorito actualizarFavorito(@PathVariable Long id, @RequestBody Favorito favorito) {
        favorito.setId(id);
        return favoritoService.save(favorito);
    }

    @DeleteMapping("/{id}")
    public void eliminarFavorito(@PathVariable Long id) {
        favoritoService.deleteById(id);
    }
}
