package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import edu.pe.residencias.model.entity.Usuario;

public interface UsuarioService {
    Usuario create(Usuario usuario);
    Usuario update(Usuario usuario);
    void delete(Long id);
    Optional<Usuario> read(Long id);
    List<Usuario> readAll();
}
