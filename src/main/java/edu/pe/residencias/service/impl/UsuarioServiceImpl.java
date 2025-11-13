package edu.pe.residencias.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.pe.residencias.model.dto.UsuarioCreateDTO;
import edu.pe.residencias.model.entity.Persona;
import edu.pe.residencias.model.entity.Rol;
import edu.pe.residencias.model.entity.Usuario;
import edu.pe.residencias.repository.PersonaRepository;
import edu.pe.residencias.repository.RolRepository;
import edu.pe.residencias.repository.UsuarioRepository;
import edu.pe.residencias.service.UsuarioService;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Usuario create(Usuario usuario) {
        return repository.save(usuario);
    }

    @Override
    @Transactional
    public Usuario createFromDTO(UsuarioCreateDTO dto) {
        Persona p = new Persona();
        p.setNombre(dto.getNombre());
        p.setApellido(dto.getApellido());
        p.setTipoDocumento(dto.getTipo_documento());
        p.setDni(dto.getDni());
        if (dto.getFecha_nacimiento() != null && !dto.getFecha_nacimiento().isEmpty()) {
            // Accept several incoming formats: plain date "yyyy-MM-dd" or date-time like "yyyy-MM-ddTHH:mm:ss(.SSS)"
            String raw = dto.getFecha_nacimiento();
            try {
                if (raw.contains("T")) {
                    // parse as LocalDateTime then convert
                    p.setFechaNacimiento(java.time.LocalDateTime.parse(raw).toLocalDate());
                } else {
                    p.setFechaNacimiento(LocalDate.parse(raw));
                }
            } catch (java.time.format.DateTimeParseException ex) {
                // As a fallback, try to extract the date portion up to 'T' or first space
                try {
                    String datePart = raw.split("T| ")[0];
                    p.setFechaNacimiento(LocalDate.parse(datePart));
                } catch (Exception ex2) {
                    // If still failing, rethrow so caller sees the error (will be logged by controller)
                    throw ex;
                }
            }
        }
        p.setTelefono(dto.getTelefono());
        p.setEmail(dto.getEmail());
        p.setDireccion(dto.getDireccion());
        p.setNotas(dto.getNotas());
        p.setSexo(dto.getSexo());
        p.setTelefonoApoderado(dto.getTelefono_apoderado());
        p.setCreatedAt(LocalDateTime.now());
        personaRepository.save(p);

        Usuario u = new Usuario();
        u.setPersona(p);
        u.setUsername(dto.getUsername());
        u.setEmailVerificado(dto.getEmail_verificado());
        u.setPassword(passwordEncoder.encode(dto.getPassword()));
        u.setEstado(dto.getEstado());
        u.setCreatedAt(LocalDateTime.now());

        Rol rol = null;
        if (dto.getRol_id() != null) {
            Optional<Rol> r = rolRepository.findById(dto.getRol_id());
            if (r.isPresent()) rol = r.get();
        }
        if (rol == null) {
            rol = rolRepository.findByNombre("inquilino").orElse(null);
        }
        u.setRol(rol);

        return repository.save(u);
    }

    @Override
    public Usuario update(Usuario usuario) {
        return repository.save(usuario);
    }

    @Override
    public void delete(java.lang.Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Usuario> read(java.lang.Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Usuario> readAll() {
        return repository.findAll();
    }

    @Override
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Override
    public Optional<Usuario> findByUsername(String username) {
        return repository.findByUsername(username);
    }

    @Override
    public Optional<Usuario> findByUuid(String uuid) {
        return repository.findByUuid(uuid);
    }

    @Override
    public Optional<Usuario> findByUsernameOrEmail(String identifier) {
        Optional<Usuario> byUsername = repository.findByUsername(identifier);
        if (byUsername.isPresent()) return byUsername;
        // try persona email
        return repository.findByPersonaEmail(identifier);
    }
}
