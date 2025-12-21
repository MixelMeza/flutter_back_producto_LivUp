package edu.pe.residencias.controller.auth;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Map;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Autowired
    private edu.pe.residencias.service.DispositivoService dispositivoService;

    @Autowired
    private edu.pe.residencias.repository.VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private edu.pe.residencias.service.EmailService emailService;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:https://flutter-back-producto-livup-2.onrender.com}")
    private String frontendBaseUrl;

    @org.springframework.beans.factory.annotation.Value("${app.mobile.scheme:}")
    private String appMobileScheme;

    @Autowired
    private edu.pe.residencias.repository.PersonaRepository personaRepository;

    @Autowired
    private edu.pe.residencias.repository.InvalidatedTokenRepository invalidatedTokenRepository;

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

            // register acceso (device) using DispositivoService + create acceso event
            try {
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();

                String fcmToken = req.getFcmToken();
                String plataforma = req.getPlataforma();
                String modelo = req.getModelo();
                String osVersion = req.getOsVersion();

                edu.pe.residencias.model.entity.Dispositivo dispositivoEntity = null;
                try {
                    System.out.println("[AuthController] device info: fcmToken='" + fcmToken + "', plataforma='" + plataforma + "', modelo='" + modelo + "', osVersion='" + osVersion + "', usuarioId=" + u.getId());
                    if (fcmToken != null && !fcmToken.isBlank()) {
                        dispositivoEntity = dispositivoService.registerOrUpdate(fcmToken, plataforma, modelo, osVersion, u.getId());
                        System.out.println("[AuthController] dispositivo registered/updated id=" + (dispositivoEntity == null ? "null" : dispositivoEntity.getId()));
                    } else {
                        System.out.println("[AuthController] No fcmToken provided in login request; skipping dispositivo register");
                    }
                } catch (Exception dEx) {
                    System.err.println("[AuthController] Warning: dispositivo register failed: " + dEx.getMessage());
                    dEx.printStackTrace();
                }

                // create acceso event (history)
                Acceso acceso = new Acceso();
                acceso.setUsuario(u);
                acceso.setUltimaSesion(edu.pe.residencias.util.DateTimeUtil.nowLima());
                acceso.setTipo("LOGIN");
                acceso.setIpAcceso(ip);
                acceso.setDispositivoRel(dispositivoEntity);
                accesoRepository.save(acceso);
                System.out.println("[AuthController] Created Acceso event id=" + acceso.getId() + " usuarioId=" + u.getId() + " dispositivoId=" + (dispositivoEntity == null ? "null" : dispositivoEntity.getId()));
            } catch (Exception ex) {
                System.err.println("[AuthController] Failed to log acceso: " + ex.getMessage());
            }

            return new ResponseEntity<>(new AuthResponse(token), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        try {
            var opt = verificationTokenRepository.findByToken(token);
            String html;
            if (opt.isEmpty()) {
                html = buildHtmlPage("Token inválido", "El token proporcionado no es válido.", false);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_HTML).body(html);
            }
            var vt = opt.get();
            if (Boolean.TRUE.equals(vt.getUsed())) {
                html = buildHtmlPage("Token ya usado", "Este enlace ya fue utilizado para verificar el correo.", false);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_HTML).body(html);
            }
            if (vt.getExpiresAt() != null && vt.getExpiresAt().isBefore(edu.pe.residencias.util.DateTimeUtil.nowLima())) {
                html = buildHtmlPage("Token expirado", "El enlace de verificación expiró. Solicita uno nuevo.", false);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_HTML).body(html);
            }

            var user = vt.getUsuario();
            user.setEmailVerificado(true);
            usuarioService.update(user);
            vt.setUsed(true);
            verificationTokenRepository.save(vt);

            // Build an HTML page that attempts to open the mobile app via custom scheme
            // If `appMobileScheme` is configured (e.g., "livup://home"), the page will
            // navigate to that scheme. Otherwise it will redirect to the frontend web URL.
            String redirectHtml = "<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                    + "<title>Verificación completada</title></head><body style=\"font-family:Inter,Roboto,Arial,sans-serif;background:#f6f9fc;color:#0f2740;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;\">"
                    + "<div style=\"text-align:center;max-width:520px;padding:24px;background:#fff;border-radius:12px;box-shadow:0 8px 24px rgba(2,30,66,0.08);\">"
                    + "<h1 style=\"margin-bottom:12px\">¡Email verificado!</h1><p style=\"margin-bottom:18px;color:#395067\">Tu correo ha sido verificado correctamente. Redirigiendo a la aplicación...</p>";

            String appTarget = (appMobileScheme != null && !appMobileScheme.isBlank()) ? appMobileScheme : "";
            String frontendTarget = (frontendBaseUrl != null && !frontendBaseUrl.isBlank()) ? frontendBaseUrl : "https://flutter-back-producto-livup-2.onrender.com";

            String js = "<script>";
            if (!"".equals(appTarget)) {
                // Try to open app scheme, then fallback to frontend after 1.2s
                js += "(function(){var opened=false; var timeout=setTimeout(function(){ if(!opened) window.location='" + frontendTarget + "'; },1200); try{ window.location='" + appTarget + "'; opened=true; }catch(e){} })();";
            } else {
                // No app scheme configured — go to frontend
                js += "window.location='" + frontendTarget + "';";
            }
            js += "</script>";

            redirectHtml += js + "</div></body></html>";
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(redirectHtml);
        } catch (Exception e) {
            e.printStackTrace();
            String html = buildHtmlPage("Error", "Ocurrió un error interno. Intenta de nuevo más tarde.", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.TEXT_HTML).body(html);
        }
    }

    // Helper to build a simple styled HTML response using Midnight Blue & Light Blue palette
    private String buildHtmlPage(String title, String message, boolean success) {
        // Colors inspired by the provided image
        String midnight = "#002b5c"; // midnight blue
        String light = "#8ec6ff"; // light blue

        String icon = success
                ? "<svg width=\"84\" height=\"84\" viewBox=\"0 0 24 24\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">"
                  + "<circle cx=\"12\" cy=\"12\" r=\"12\" fill=\"" + light + "\"/>"
                  + "<path d=\"M6.5 12.5l3.2 3.2L17.5 8.9\" stroke=\"" + midnight + "\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>"
                  + "</svg>"
                : "<svg width=\"84\" height=\"84\" viewBox=\"0 0 24 24\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">"
                  + "<circle cx=\"12\" cy=\"12\" r=\"12\" fill=\"" + light + "\"/>"
                  + "<path d=\"M8 8l8 8M16 8l-8 8\" stroke=\"" + midnight + "\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>"
                  + "</svg>";

        String html = "<!doctype html>" +
                "<html lang=\"es\">" +
                "<head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<title>Verificación de correo</title>" +
                "<style>body{font-family:Inter,Roboto,Arial,sans-serif;background:#f4f8ff;color:#102135;margin:0;padding:0} .container{max-width:720px;margin:64px auto;background:white;border-radius:12px;box-shadow:0 10px 30px rgba(2,30,66,0.06);padding:48px;text-align:center;border-top:8px solid " + light + ";} h1{color:" + midnight + ";margin:18px 0 8px;font-size:24px} p{color:#324a63;margin:0 0 6px;font-size:16px;line-height:1.4} footer{margin-top:28px;color:#6b7a8a;font-size:13px}</style></head>" +
                "<body><div class=\"container\">" + icon + "<h1>" + title + "</h1><p>" + message + "</p>";

            // No buttons as requested
            html += "<footer>LivUp &copy; " + java.time.Year.now().getValue() + "</footer></div></body></html>";
            return html;
    }

    // Build a friendly, responsive HTML verification email with CTA button and plain-text fallback
    private String buildVerificationEmailHtml(String toEmail, String verifyLink) {
        String primary = "#0066CC";
        String bg = "#f6f9fc";
        String textColor = "#0f2740";

        // Minimal, well-formed responsive template with preheader and clear CTA
        String html = "<!doctype html>" +
            "<html lang=\"es\">" +
            "<head>" +
            "<meta charset=\"utf-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
            "<title>Verifica tu correo</title>" +
            "<style>" +
            "  body{margin:0;padding:0;background:" + bg + ";font-family:Inter,Roboto,Arial,sans-serif;color:" + textColor + ";-webkit-font-smoothing:antialiased;}" +
            "  .container{width:100%;max-width:680px;margin:0 auto;padding:20px;}" +
            "  .card{background:#ffffff;border-radius:12px;padding:28px;box-shadow:0 8px 24px rgba(2,30,66,0.06);}" +
            "  h1{font-size:20px;margin:0 0 12px;color:" + textColor + ";}" +
            "  p{margin:0 0 16px;color:#395067;line-height:1.5;}" +
            "  .btn{display:inline-block;padding:12px 20px;background:" + primary + ";color:#ffffff;border-radius:8px;text-decoration:none;font-weight:600;}" +
            "  .muted{color:#7b8b98;font-size:13px;margin-top:18px;}" +
            "  @media (max-width:480px){.card{padding:18px}.btn{width:100%;display:block;text-align:center}}" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class=\"container\">" +
            "  <div class=\"card\">" +
            "    <div style=\"text-align:center;margin-bottom:16px\">" +
            "      <img src=\"https://raw.githubusercontent.com/your-repo/assets/main/logo.png\" alt=\"LivUp\" style=\"height:46px;display:block;margin:0 auto\">" +
            "    </div>" +
            "    <h1>Verifica tu correo electrónico</h1>" +
            "    <p>Hola, gracias por registrarte en LivUp. Para activar tu cuenta pulsa el botón de abajo:</p>" +
            "    <p style=\"text-align:center;margin:20px 0\">" +
            "      <a class=\"btn\" href=\"" + verifyLink + "\" target=\"_blank\">Verificar mi correo</a>" +
            "    </p>" +
            // Visible raw link removed — link is only present in the CTA button above
            "    <p class=\"muted\">Si no solicitaste esta verificación, puedes ignorar este correo.</p>" +
            "  </div>" +
            "  <p style=\"text-align:center;color:#9aa7b8;font-size:12px;margin-top:12px\">LivUp &copy; " + java.time.Year.now().getValue() + "</p>" +
            "</div>" +
            "</body></html>";

        return html;
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(HttpServletRequest request) {
        try {
            // Extraer usuario del JWT
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return new ResponseEntity<>(new ErrorResponse("Token de autorización requerido"), HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            Claims claims = jwtUtil.parseToken(token);
            String username = claims.get("user", String.class);
            
            Optional<Usuario> userOpt = usuarioService.findByUsernameOrEmail(username);
            if (userOpt.isEmpty()) {
                return new ResponseEntity<>(new ErrorResponse("Usuario no encontrado"), HttpStatus.NOT_FOUND);
            }

            var user = userOpt.get();
            
            // Validar que el usuario tenga Persona y email
            if (user.getPersona() == null || user.getPersona().getEmail() == null || user.getPersona().getEmail().isBlank()) {
                return new ResponseEntity<>(new ErrorResponse("El usuario no tiene un correo registrado"), HttpStatus.BAD_REQUEST);
            }
            
            if (Boolean.TRUE.equals(user.getEmailVerificado())) {
                return new ResponseEntity<>(new ErrorResponse("Tu correo ya está verificado"), HttpStatus.BAD_REQUEST);
            }

            // Buscar un token válido existente (no usado y no expirado)
            var tokens = verificationTokenRepository.findByUsuario(user);
            var now = edu.pe.residencias.util.DateTimeUtil.nowLima();
            edu.pe.residencias.model.entity.VerificationToken chosen = null;
            if (tokens != null) {
                for (var t : tokens) {
                    if (!Boolean.TRUE.equals(t.getUsed())) {
                        if (t.getExpiresAt() == null || t.getExpiresAt().isAfter(now)) {
                            chosen = t; // reuse existing valid token
                            break;
                        }
                    }
                }
            }

            // Si no hay token válido, invalidar antiguos y crear uno nuevo
            if (chosen == null) {
                if (tokens != null) {
                    for (var t : tokens) {
                        if (!Boolean.TRUE.equals(t.getUsed())) {
                            t.setUsed(true);
                            verificationTokenRepository.save(t);
                        }
                    }
                }

                String verificationToken = java.util.UUID.randomUUID().toString();
                chosen = new edu.pe.residencias.model.entity.VerificationToken();
                chosen.setToken(verificationToken);
                chosen.setUsuario(user);
                chosen.setCreatedAt(now);
                chosen.setExpiresAt(now.plusHours(24));
                chosen.setUsed(false);
                verificationTokenRepository.save(chosen);
            }

            // Enviar email de verificación usando el token elegido/creado
            try {
                String verifyLink = frontendBaseUrl + "/api/auth/verify?token=" + chosen.getToken();
                String subject = "Verifica tu correo en LivUp";
                String plainText = "Para verificar tu correo haz clic en: " + verifyLink;
                String html = buildVerificationEmailHtml(user.getPersona().getEmail(), verifyLink);
                emailService.sendHtmlMessage(user.getPersona().getEmail(), subject, html, plainText);
            } catch (Exception emailEx) {
                System.err.println("[AuthController] Error al enviar email: " + emailEx.getMessage());
                return new ResponseEntity<>(new ErrorResponse("No se pudo enviar el correo de verificación. Verifica que tu email sea válido."), HttpStatus.SERVICE_UNAVAILABLE);
            }

            return new ResponseEntity<>("Correo de verificación enviado exitosamente a " + user.getPersona().getEmail(), HttpStatus.OK);
        } catch (io.jsonwebtoken.JwtException jwtEx) {
            System.err.println("[AuthController] Error JWT: " + jwtEx.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Token de autenticación inválido o expirado"), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ErrorResponse("Error al reenviar verificación. Intenta de nuevo más tarde."), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/send-verification-by-token")
    public ResponseEntity<?> sendVerificationByToken(@RequestBody java.util.Map<String, String> body) {
        try {
            String token = body.get("token");
            if (token == null || token.isBlank()) {
                return new ResponseEntity<>(new ErrorResponse("El token es requerido"), HttpStatus.BAD_REQUEST);
            }
            var opt = verificationTokenRepository.findByToken(token);
            if (opt.isEmpty()) {
                return new ResponseEntity<>(new ErrorResponse("Token no encontrado"), HttpStatus.NOT_FOUND);
            }
            var vt = opt.get();
            var user = vt.getUsuario();
            if (user == null || user.getPersona() == null || user.getPersona().getEmail() == null || user.getPersona().getEmail().isBlank()) {
                return new ResponseEntity<>(new ErrorResponse("El usuario asociado al token no tiene correo registrado"), HttpStatus.BAD_REQUEST);
            }
            if (Boolean.TRUE.equals(vt.getUsed())) {
                return new ResponseEntity<>(new ErrorResponse("El token ya fue usado"), HttpStatus.BAD_REQUEST);
            }
            if (vt.getExpiresAt() != null && vt.getExpiresAt().isBefore(edu.pe.residencias.util.DateTimeUtil.nowLima())) {
                return new ResponseEntity<>(new ErrorResponse("El token ha expirado"), HttpStatus.BAD_REQUEST);
            }

            try {
                String verifyLink = frontendBaseUrl + "/api/auth/verify?token=" + token;
                String subject = "Verifica tu correo en LivUp";
                String plainText = "Para verificar tu correo haz clic en: " + verifyLink;
                String html = buildVerificationEmailHtml(user.getPersona().getEmail(), verifyLink);
                emailService.sendHtmlMessage(user.getPersona().getEmail(), subject, html, plainText);
            } catch (Exception emailEx) {
                System.err.println("[AuthController] Error al enviar email por token: " + emailEx.getMessage());
                return new ResponseEntity<>(new ErrorResponse("No se pudo enviar el correo de verificación"), HttpStatus.SERVICE_UNAVAILABLE);
            }

            return new ResponseEntity<>("Correo de verificación reenviado correctamente a " + user.getPersona().getEmail(), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ErrorResponse("Error interno al reenviar correo de verificación"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/change-email")
    public ResponseEntity<?> changeEmail(@RequestBody java.util.Map<String, String> body, HttpServletRequest request) {
        try {
            String newEmail = body.get("newEmail");
            
            if (newEmail == null || newEmail.isBlank()) {
                return new ResponseEntity<>(new ErrorResponse("El nuevo correo es requerido"), HttpStatus.BAD_REQUEST);
            }
            
            newEmail = newEmail.trim().toLowerCase();

            // Validar formato de email
            String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
            if (!newEmail.matches(emailRegex)) {
                return new ResponseEntity<>(new ErrorResponse("El formato del correo es inválido. Ejemplo: usuario@ejemplo.com"), HttpStatus.BAD_REQUEST);
            }

            // Extraer usuario del JWT
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return new ResponseEntity<>(new ErrorResponse("Token de autorización requerido"), HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            Claims claims = jwtUtil.parseToken(token);
            String username = claims.get("user", String.class);
            
            Optional<Usuario> userOpt = usuarioService.findByUsernameOrEmail(username);
            if (userOpt.isEmpty()) {
                return new ResponseEntity<>(new ErrorResponse("Usuario no encontrado"), HttpStatus.NOT_FOUND);
            }

            Usuario user = userOpt.get();
            
            // Validar que el usuario tenga Persona
            if (user.getPersona() == null) {
                return new ResponseEntity<>(new ErrorResponse("El usuario no tiene datos personales registrados"), HttpStatus.BAD_REQUEST);
            }
            
            // Verificar si es el mismo email actual
            String currentEmail = user.getPersona().getEmail();
            if (newEmail.equalsIgnoreCase(currentEmail)) {
                return new ResponseEntity<>(new ErrorResponse("El nuevo correo es igual al actual"), HttpStatus.BAD_REQUEST);
            }
            
            // Verificar que el nuevo email no esté en uso
            Optional<Usuario> existingUser = usuarioService.findByUsernameOrEmail(newEmail);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                return new ResponseEntity<>(new ErrorResponse("El correo " + newEmail + " ya está registrado por otro usuario"), HttpStatus.CONFLICT);
            }

            // Actualizar email en Persona
            var persona = user.getPersona();
            String oldEmail = persona.getEmail();
            persona.setEmail(newEmail);
            
            // Guardar Persona primero (es una entidad independiente)
            personaRepository.save(persona);
            
            // Marcar como no verificado y actualizar Usuario
            user.setEmailVerificado(false);
            usuarioService.update(user);
            
            System.out.println("[AuthController] Email actualizado de " + oldEmail + " a " + newEmail + " para usuario: " + user.getUsername());

            // Invalidar tokens anteriores
            var oldTokens = verificationTokenRepository.findByUsuario(user);
            for (var oldToken : oldTokens) {
                if (!Boolean.TRUE.equals(oldToken.getUsed())) {
                    oldToken.setUsed(true);
                    verificationTokenRepository.save(oldToken);
                }
            }

            // Crear nuevo token de verificación
            String verificationToken = java.util.UUID.randomUUID().toString();
            edu.pe.residencias.model.entity.VerificationToken vt = new edu.pe.residencias.model.entity.VerificationToken();
            vt.setToken(verificationToken);
            vt.setUsuario(user);
            vt.setCreatedAt(edu.pe.residencias.util.DateTimeUtil.nowLima());
            vt.setExpiresAt(edu.pe.residencias.util.DateTimeUtil.nowLima().plusHours(24));
            vt.setUsed(false);
            verificationTokenRepository.save(vt);

            // Enviar email de verificación al nuevo correo
            try {
                String verifyLink = frontendBaseUrl + "/api/auth/verify?token=" + verificationToken;
                String subject = "Verifica tu nuevo correo en LivUp";
                String plainText = "Has cambiado tu correo en LivUp. Para verificar tu nuevo correo haz clic en: " + verifyLink;
                String html = buildVerificationEmailHtml(newEmail, verifyLink);
                emailService.sendHtmlMessage(newEmail, subject, html, plainText);
            } catch (Exception emailEx) {
                System.err.println("[AuthController] Error al enviar email de verificación: " + emailEx.getMessage());
                return new ResponseEntity<>(new ErrorResponse("Email actualizado pero no se pudo enviar el correo de verificación. Verifica que el correo sea válido."), HttpStatus.PARTIAL_CONTENT);
            }

            return new ResponseEntity<>("Correo actualizado exitosamente. Se ha enviado un email de verificación a " + newEmail, HttpStatus.OK);
        } catch (io.jsonwebtoken.JwtException jwtEx) {
            System.err.println("[AuthController] Error JWT: " + jwtEx.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Token de autenticación inválido o expirado"), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ErrorResponse("Error al cambiar email"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody java.util.Map<String, String> body, HttpServletRequest request) {
        try {
            String currentPassword = body.get("currentPassword");
            String newPassword = body.get("newPassword");
            
            if (currentPassword == null || newPassword == null || currentPassword.isBlank() || newPassword.isBlank()) {
                return new ResponseEntity<>(new ErrorResponse("Contraseña actual y nueva contraseña son requeridas"), HttpStatus.BAD_REQUEST);
            }

            // Validate new password strength
            if (newPassword.length() < 6) {
                return new ResponseEntity<>(new ErrorResponse("La nueva contraseña debe tener al menos 6 caracteres"), HttpStatus.BAD_REQUEST);
            }

            // Extract user from JWT token
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return new ResponseEntity<>(new ErrorResponse("Token de autorización requerido"), HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            Claims claims = jwtUtil.parseToken(token);
            String username = claims.get("user", String.class);
            
            Optional<Usuario> userOpt = usuarioService.findByUsernameOrEmail(username);
            if (userOpt.isEmpty()) {
                return new ResponseEntity<>(new ErrorResponse("Usuario no encontrado"), HttpStatus.NOT_FOUND);
            }

            Usuario user = userOpt.get();
            
            // Verify current password
            if (!usuarioService.matchesPassword(currentPassword, user.getPassword())) {
                return new ResponseEntity<>(new ErrorResponse("Contraseña actual incorrecta"), HttpStatus.UNAUTHORIZED);
            }

            // Update password
            user.setPassword(usuarioService.encodePassword(newPassword));
            usuarioService.update(user);

            return new ResponseEntity<>("Contraseña actualizada exitosamente", HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ErrorResponse("Error interno del servidor"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) java.util.Map<String, String> body, HttpServletRequest request) {
        try {
            // Extract user from JWT
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return new ResponseEntity<>(new ErrorResponse("Token de autorización requerido"), HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            Claims claims = jwtUtil.parseToken(token);
            String username = claims.get("user", String.class);

            Optional<Usuario> userOpt = usuarioService.findByUsernameOrEmail(username);
            if (userOpt.isEmpty()) {
                return new ResponseEntity<>(new ErrorResponse("Usuario no encontrado"), HttpStatus.NOT_FOUND);
            }

            Usuario user = userOpt.get();

            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();

            String fcmToken = null;
            if (body != null) {
                fcmToken = body.get("fcmToken");
            }

            edu.pe.residencias.model.entity.Dispositivo dispositivoEntity = null;
            try {
                if (fcmToken != null && !fcmToken.isBlank()) {
                    dispositivoEntity = dispositivoService.findByFcmToken(fcmToken).orElse(null);
                }
            } catch (Exception dEx) {
                System.err.println("[AuthController] Warning: dispositivo lookup failed on logout: " + dEx.getMessage());
            }

            // Create Acceso event for logout
            try {
                Acceso acceso = new Acceso();
                acceso.setUsuario(user);
                acceso.setUltimaSesion(edu.pe.residencias.util.DateTimeUtil.nowLima());
                acceso.setTipo("LOGOUT");
                acceso.setIpAcceso(ip);
                acceso.setDispositivoRel(dispositivoEntity);
                accesoRepository.save(acceso);
                System.out.println("[AuthController] Created Acceso (logout) id=" + acceso.getId() + " usuarioId=" + user.getId());
            } catch (Exception ex) {
                System.err.println("[AuthController] Failed to log acceso (logout): " + ex.getMessage());
            }

            // Do NOT delete/unregister the dispositivo on logout.
            // Keeping dispositivo records ensures the FCM token remains tied to the physical device.
            // Token reassignment (if a different user logs in on the same device) is handled by registerOrUpdate on login.
            if (dispositivoEntity != null) {
                System.out.println("[AuthController] Logout: dispositivo preserved for token=" + dispositivoEntity.getFcmToken());
            }

            // Invalidate JWT token (store in invalidated tokens table) so requests using it are rejected
            try {
                if (token != null && !token.isBlank()) {
                    // avoid duplicate entries
                    invalidatedTokenRepository.findByToken(token).orElseGet(() -> {
                        edu.pe.residencias.model.entity.InvalidatedToken it = new edu.pe.residencias.model.entity.InvalidatedToken();
                        it.setToken(token);
                        it.setCreatedAt(edu.pe.residencias.util.DateTimeUtil.nowLima());
                        return invalidatedTokenRepository.save(it);
                    });
                }
            } catch (Exception invEx) {
                System.err.println("[AuthController] Warning: failed to persist invalidated token: " + invEx.getMessage());
            }

            return new ResponseEntity<>(Map.of("message", "Sesión cerrada correctamente"), HttpStatus.OK);
        } catch (io.jsonwebtoken.JwtException jwtEx) {
            System.err.println("[AuthController] Error JWT: " + jwtEx.getMessage());
            return new ResponseEntity<>(new ErrorResponse("Token de autenticación inválido o expirado"), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ErrorResponse("Error al cerrar sesión"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
