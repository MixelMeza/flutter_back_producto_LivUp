package edu.pe.residencias.controller.auth;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pe.residencias.model.entity.Acceso;
import edu.pe.residencias.model.entity.Usuario;
import edu.pe.residencias.repository.AccesoRepository;
import edu.pe.residencias.security.JwtUtil;
import edu.pe.residencias.service.UsuarioService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AccesoRepository accesoRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req, HttpServletRequest request) {
        try {
            String identifier = req.getUsername(); // may be username or email from frontend
            // log incoming request for debugging (mask password)
            String rawPwd = req.getPassword() == null ? "" : req.getPassword();
            String pwdMask = rawPwd.isEmpty() ? "" : "*".repeat(Math.min(rawPwd.length(), 4)) + "(" + rawPwd.length() + ")";
            String ipForLog = request.getHeader("X-Forwarded-For");
            if (ipForLog == null || ipForLog.isEmpty()) {
                ipForLog = request.getRemoteAddr();
            }
            String uaForLog = request.getHeader("User-Agent");
            System.out.println("[AuthController] Login attempt - identifier='" + identifier + "', password='" + pwdMask + "', ip='" + ipForLog + "', ua='" + uaForLog + "'");
            Optional<Usuario> opt = usuarioService.findByUsernameOrEmail(identifier);
            if (opt.isEmpty()) {
                // log failed attempt
                System.out.println("[AuthController] Login failed - user not found: " + identifier);
                return new ResponseEntity<>(new ErrorResponse("Usuario no encontrado"), HttpStatus.UNAUTHORIZED);
            }
            Usuario u = opt.get();
            if (!usuarioService.matchesPassword(req.getPassword(), u.getPassword())) {
                System.out.println("[AuthController] Login failed - invalid password for: " + identifier);
                return new ResponseEntity<>(new ErrorResponse("Contraseña incorrecta"), HttpStatus.UNAUTHORIZED);
            }

            // generate token based on uuid column and include encrypted username and role
            String roleName = u.getRol() == null ? null : u.getRol().getNombre();
            String token = jwtUtil.generateToken(java.util.UUID.fromString(u.getUuid()), u.getUsername(), roleName);

            // register acceso (device + ip)
            try {
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
                String userAgent = request.getHeader("User-Agent");

                Acceso acceso = new Acceso();
                acceso.setUsuario(u);
                acceso.setUltimaSesion(LocalDateTime.now());
                acceso.setIpAcceso(ip);
                acceso.setDispositivo(userAgent);
                System.out.println("[AuthController] Acceso to save: usuarioId=" + (u.getId() == null ? "null" : u.getId()) + ", uuid=" + u.getUuid() + ", ip=" + ip + ", dispositivo=" + userAgent + ", ultimaSesion=" + LocalDateTime.now());
                accesoRepository.save(acceso);
            } catch (Exception ex) {
                // don't fail login if access logging fails, but log to console
                System.err.println("[AuthController] Failed to log acceso: " + ex.getMessage());
            }

            return new ResponseEntity<>(new AuthResponse(token), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
