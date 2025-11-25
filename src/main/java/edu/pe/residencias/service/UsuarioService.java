package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;
import java.lang.Long;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import edu.pe.residencias.model.dto.UsuarioCreateDTO;
import edu.pe.residencias.model.entity.Usuario;
import edu.pe.residencias.model.dto.UsuarioAdminDTO;

public interface UsuarioService {
    Usuario create(Usuario usuario);
    // create a user along with persona and encrypt password
    Usuario createFromDTO(UsuarioCreateDTO dto);
    Usuario update(Usuario usuario);
    void delete(Long id);
    Optional<Usuario> read(Long id);
    List<Usuario> readAll();

    // find by username or by persona email (login identifier)
    Optional<Usuario> findByUsernameOrEmail(String identifier);

    Optional<Usuario> findByUuid(String uuid);

    edu.pe.residencias.model.dto.UserProfileDTO getProfileByUuid(String uuid);

    edu.pe.residencias.model.dto.UserProfileDTO updateProfileByUuid(String uuid, edu.pe.residencias.model.dto.PersonaUpdateDTO dto);

    // password helpers
    String encodePassword(String rawPassword);
    boolean matchesPassword(String rawPassword, String encodedPassword);
    Optional<Usuario> findByUsername(String username);
    
    // Nuevos métodos
    Page<Usuario> findAllPaginated(Pageable pageable);
    List<UsuarioAdminDTO> mapToUsuarioAdminDTOs(List<Usuario> usuarios);
}
