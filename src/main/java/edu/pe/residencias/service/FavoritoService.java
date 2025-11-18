package edu.pe.residencias.service;

public interface FavoritoService {
    void like(Long usuarioId, Long habitacionId);
    void unlike(Long usuarioId, Long habitacionId);
    boolean isLiked(Long usuarioId, Long habitacionId);
    long countLikes(Long habitacionId);
    java.util.List<edu.pe.residencias.model.entity.Favorito> findAll();
    edu.pe.residencias.model.entity.Favorito save(edu.pe.residencias.model.entity.Favorito favorito);
    edu.pe.residencias.model.entity.Favorito findById(Long id);
    void deleteById(Long id);
}
