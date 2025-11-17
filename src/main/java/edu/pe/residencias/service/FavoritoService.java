package edu.pe.residencias.service;

public interface FavoritoService {
    void like(Long usuarioId, Long habitacionId);
    void unlike(Long usuarioId, Long habitacionId);
    boolean isLiked(Long usuarioId, Long habitacionId);
    long countLikes(Long habitacionId);
}
