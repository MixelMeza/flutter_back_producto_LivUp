package edu.pe.residencias.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.validation.Valid;
import edu.pe.residencias.model.dto.UsuarioCreateDTO;
import edu.pe.residencias.model.entity.Usuario;
import edu.pe.residencias.model.dto.UsuarioAdminDTO;
import edu.pe.residencias.service.UsuarioService;
import edu.pe.residencias.model.dto.UserProfileDTO;
import edu.pe.residencias.security.JwtUtil;
import edu.pe.residencias.controller.auth.ErrorResponse;
import io.jsonwebtoken.Claims;
import org.springframework.web.bind.annotation.RequestHeader;
import edu.pe.residencias.model.dto.PersonaUpdateDTO;
import edu.pe.residencias.exception.DuplicateRegistrationException;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    
    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private edu.pe.residencias.repository.DispositivoRepository dispositivoRepository;

    @Autowired
    private edu.pe.residencias.repository.AccesoRepository accesoRepository;

    // personaRepository is handled inside the service now

    @GetMapping
    public ResponseEntity<List<Usuario>> readAll() {
        try {
            List<Usuario> usuarios = usuarioService.readAll();
            if (usuarios.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(usuarios, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody UsuarioCreateDTO usuarioDto) {
        try {
            // Log incoming registration data (mask password length)
            String pwd = usuarioDto.getPassword() == null ? "" : usuarioDto.getPassword();
            String pwdMask = pwd.isEmpty() ? "" : "*".repeat(Math.min(pwd.length(), 4)) + "(" + pwd.length() + ")";
            System.out.println("[UsuarioController] Registro attempt - username='" + usuarioDto.getUsername() + "', email='" + usuarioDto.getEmail() + "', password='" + pwdMask + "', nombre='" + usuarioDto.getNombre() + "', apellido='" + usuarioDto.getApellido() + "', dni='" + usuarioDto.getDni() + "'");

            Usuario u = usuarioService.createFromDTO(usuarioDto);
            System.out.println("[UsuarioController] Registro success - usuarioId=" + (u.getId() == null ? "null" : u.getId()) + ", uuid=" + u.getUuid());
            return new ResponseEntity<>(u, HttpStatus.CREATED);
        } catch (DuplicateRegistrationException dup) {
            return new ResponseEntity<>(new ErrorResponse(dup.getMessage()), HttpStatus.CONFLICT);
        } catch (Exception e) {
            // Print stacktrace to console so you can see the error cause
            System.err.println("[UsuarioController] Registro failed: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || authHeader.isEmpty()) {
                return new ResponseEntity<>(new ErrorResponse("No token provided"), HttpStatus.UNAUTHORIZED);
            }
            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
            // Log request for debugging (mask token tail)
            String tokenShort = token.length() > 20 ? token.substring(0, 10) + "..." + token.substring(token.length()-10) : token;
            System.out.println("[UsuarioController.me] token=" + tokenShort);
            Claims claims = jwtUtil.parseToken(token);
            String uuid = claims.get("uid", String.class);
            if (uuid == null) {
                return new ResponseEntity<>(new ErrorResponse("Invalid token"), HttpStatus.UNAUTHORIZED);
            }
            UserProfileDTO profile = usuarioService.getProfileByUuid(uuid);
            if (profile == null) {
                return new ResponseEntity<>(new ErrorResponse("User not found"), HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(profile, HttpStatus.OK);
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            return new ResponseEntity<>(new ErrorResponse("Token expired"), HttpStatus.UNAUTHORIZED);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(new ErrorResponse("Failed to parse token"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/me/dispositivos")
    public ResponseEntity<?> myDevices(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || authHeader.isEmpty()) {
                return new ResponseEntity<>(new ErrorResponse("No token provided"), HttpStatus.UNAUTHORIZED);
            }
            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
            Claims claims = jwtUtil.parseToken(token);
            String uuid = claims.get("uid", String.class);
            if (uuid == null) return new ResponseEntity<>(new ErrorResponse("Invalid token"), HttpStatus.UNAUTHORIZED);
            var usuarioOpt = usuarioService.findByUuid(uuid);
            if (usuarioOpt.isEmpty()) return new ResponseEntity<>(new ErrorResponse("User not found"), HttpStatus.NOT_FOUND);
            var usuario = usuarioOpt.get();

            // load active dispositivos for this usuario
            java.util.List<edu.pe.residencias.model.entity.Dispositivo> devices = dispositivoRepository.findByUsuarioIdAndActivoTrue(usuario.getId());
            java.util.List<edu.pe.residencias.model.dto.DispositivoInfoDTO> out = new java.util.ArrayList<>();
            for (var d : devices) {
                try {
                    edu.pe.residencias.model.dto.DispositivoInfoDTO dto = new edu.pe.residencias.model.dto.DispositivoInfoDTO();
                    dto.setId(d.getId());
                    dto.setFcmToken(d.getFcmToken());
                    dto.setPlataforma(d.getPlataforma());
                    dto.setModelo(d.getModelo());
                    dto.setNombre((d.getPlataforma() == null ? "" : d.getPlataforma()) + " " + (d.getModelo() == null ? "" : d.getModelo()));
                    dto.setActivo(d.getActivo());
                    dto.setPrimeroVisto(d.getCreatedAt());

                    // find latest acceso for this user+device
                    var accOpt = accesoRepository.findFirstByUsuarioIdAndDispositivoRelIdOrderByUltimaSesionDesc(usuario.getId(), d.getId());
                    if (accOpt.isPresent()) {
                        var acc = accOpt.get();
                        dto.setUltimoAcceso(acc.getUltimaSesion());
                        dto.setUltimoTipo(acc.getTipo());
                        dto.setEstaLogueado("LOGIN".equalsIgnoreCase(acc.getTipo()));
                    } else {
                        dto.setUltimoAcceso(null);
                        dto.setUltimoTipo(null);
                        dto.setEstaLogueado(false);
                    }

                    out.add(dto);
                } catch (Exception ex) {
                    // skip device on error
                }
            }

            if (out.isEmpty()) return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            return ResponseEntity.ok(out);
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            return new ResponseEntity<>(new ErrorResponse("Token expired"), HttpStatus.UNAUTHORIZED);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(new ErrorResponse("Failed to list devices"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                      @Valid @RequestBody PersonaUpdateDTO updateDto) {
        try {
            if (authHeader == null || authHeader.isEmpty()) {
                return new ResponseEntity<>(new ErrorResponse("No token provided"), HttpStatus.UNAUTHORIZED);
            }
            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
            String tokenShort = token.length() > 20 ? token.substring(0, 10) + "..." + token.substring(token.length()-10) : token;
            System.out.println("[UsuarioController.updateMe] token=" + tokenShort + ", payload=" + updateDto);
            Claims claims = jwtUtil.parseToken(token);
            String uuid = claims.get("uid", String.class);
            if (uuid == null) {
                return new ResponseEntity<>(new ErrorResponse("Invalid token"), HttpStatus.UNAUTHORIZED);
            }
            UserProfileDTO profileDto = usuarioService.updateProfileByUuid(uuid, updateDto);
            if (profileDto == null) return new ResponseEntity<>(new ErrorResponse("User not found"), HttpStatus.NOT_FOUND);
            return new ResponseEntity<>(profileDto, HttpStatus.OK);
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            return new ResponseEntity<>(new ErrorResponse("Token expired"), HttpStatus.UNAUTHORIZED);
        } catch (org.springframework.dao.DataIntegrityViolationException dex) {
            dex.printStackTrace();
            // likely unique constraint violation (email, dni, etc.)
            return new ResponseEntity<>(new ErrorResponse("Data integrity error: " + dex.getMostSpecificCause().getMessage()), HttpStatus.CONFLICT);
        } catch (Exception ex) {
            // capture full stacktrace
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            ex.printStackTrace(pw);
            String stack = sw.toString();
            // log and return stacktrace in response for debugging (remove in production)
            System.err.println("[UsuarioController.updateMe] Exception: " + stack);
            ErrorResponse resp = new ErrorResponse(stack);
            return new ResponseEntity<>(resp, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> getUsuarioId(@PathVariable("id") Long id) {
        try {
            Optional<Usuario> opt = usuarioService.read(id);
            if (opt.isEmpty()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            Usuario u = opt.get();
            return new ResponseEntity<>(u, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Usuario> delUsuario(@PathVariable("id") Long id) {
        try {
            usuarioService.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUsuario(@PathVariable("id") Long id, @Valid @RequestBody Usuario usuario) {
        Optional<Usuario> u = usuarioService.read(id);
        if (u.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            Usuario updatedUsuario = usuarioService.update(usuario);
            return new ResponseEntity<>(updatedUsuario, HttpStatus.OK);
        }
    }

    @PatchMapping("/me")
    public ResponseEntity<?> updateMyProfile(@Valid @RequestBody PersonaUpdateDTO updateDTO, 
                                           @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || authHeader.isEmpty()) {
                return new ResponseEntity<>(new ErrorResponse("No token provided"), HttpStatus.UNAUTHORIZED);
            }
            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
            Claims claims = jwtUtil.parseToken(token);
            String uuid = claims.get("uid", String.class);
            
            if (uuid == null) {
                return new ResponseEntity<>(new ErrorResponse("Invalid token"), HttpStatus.UNAUTHORIZED);
            }

            UserProfileDTO updatedProfile = usuarioService.updateProfileByUuid(uuid, updateDTO);
            if (updatedProfile == null) {
                return new ResponseEntity<>(new ErrorResponse("User not found"), HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(updatedProfile, HttpStatus.OK);
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            return new ResponseEntity<>(new ErrorResponse("Token expired"), HttpStatus.UNAUTHORIZED);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(new ErrorResponse("Failed to update profile"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // NUEVO: Listado paginado de todos los usuarios (ADMIN)
    @GetMapping("/admin/paginated")
    public ResponseEntity<?> getUsuariosPaginatedAdmin(Pageable pageable) {
        try {
            Page<Usuario> usuariosPage = usuarioService.findAllPaginated(pageable);
            if (usuariosPage.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            // Convertir a DTOs
            List<UsuarioAdminDTO> dtos = usuarioService.mapToUsuarioAdminDTOs(usuariosPage.getContent());
            
            // Crear respuesta con paginación
            java.util.HashMap<String, Object> response = new java.util.HashMap<>();
            response.put("content", dtos);
            response.put("totalElements", usuariosPage.getTotalElements());
            response.put("totalPages", usuariosPage.getTotalPages());
            response.put("currentPage", usuariosPage.getNumber());
            response.put("pageSize", usuariosPage.getSize());
            response.put("hasNext", usuariosPage.hasNext());
            response.put("hasPrevious", usuariosPage.hasPrevious());

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
