package edu.pe.residencias.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import edu.pe.residencias.repository.InvalidatedTokenRepository;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private InvalidatedTokenRepository invalidatedTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        // Allow preflight requests through so CORS works without requiring Authorization
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        // Exclude login, register, verification and other public endpoints from JWT requirement
        if ("/api/auth/login".equals(path)
            || "/api/auth/register".equals(path)
            || "/api/auth/verify".equals(path)
            || "/api/auth/send-verification-by-token".equals(path)
            || "/api/auth/resend-verification-by-email".equals(path)
            // Public registration endpoint used by the mobile app
            || ("/api/usuarios".equals(path) && "POST".equalsIgnoreCase(request.getMethod()))
            || "/api/public/keepalive".equals(path)
            || "/api/public/app-status".equals(path)
            || "/api/search".equals(path)
            || "/api/solicitudes-alojamiento/vencidas".equals(path)
            || "/api/public/backups/run".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        try {
            // If token was invalidated (logout), reject immediately
            if (invalidatedTokenRepository.findByToken(token).isPresent()) {
                // DEBUG: log rejected token and request path
                System.out.println("[JwtRequestFilter] Rejected invalidated token for path=" + path + " token=" + (token == null ? "<null>" : token));
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token invalidated");
                return;
            }

            var claims = jwtUtil.parseToken(token);
            // expose claims for controllers if needed
            request.setAttribute("jwtClaims", claims);
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid token");
        }
    }
}
