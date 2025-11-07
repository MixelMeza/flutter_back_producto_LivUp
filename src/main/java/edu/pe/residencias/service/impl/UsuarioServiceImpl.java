package edu.pe.residencias.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.Usuario;
import edu.pe.residencias.repository.UsuarioRepository;
import edu.pe.residencias.service.UsuarioService;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository repository;

    @Override
    public Usuario create(Usuario usuario) {
        return repository.save(usuario);
    }

    @Override
    public Usuario update(Usuario usuario) {
        return repository.save(usuario);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Usuario> read(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Usuario> readAll() {
        return repository.findAll();
    }
}
