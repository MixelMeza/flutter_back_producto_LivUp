package edu.pe.residencias.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.pe.residencias.model.entity.Feedback;
import edu.pe.residencias.model.entity.Usuario;
import edu.pe.residencias.repository.FeedbackRepository;
import edu.pe.residencias.security.JwtUtil;
import edu.pe.residencias.service.UsuarioService;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioService usuarioService;

    public Feedback create(Feedback f) {
        return feedbackRepository.save(f);
    }

    public List<Feedback> listAll() { return feedbackRepository.findAll(); }
    public List<Feedback> findByTipo(Feedback.Tipo t) { return feedbackRepository.findByTipo(t); }
    public List<Feedback> findByEstado(Feedback.Estado e) { return feedbackRepository.findByEstado(e); }
    public Optional<Feedback> findById(Long id) { return feedbackRepository.findById(id); }
    public Feedback save(Feedback f) { return feedbackRepository.save(f); }
    public void delete(Long id) { feedbackRepository.deleteById(id); }

    public Optional<Usuario> getAdminFromRequest(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) return Optional.empty();
            String token = authHeader.substring(7);
            var claims = jwtUtil.parseToken(token);
            String username = claims.get("user", String.class);
            var userOpt = usuarioService.findByUsernameOrEmail(username);
            if (userOpt.isEmpty()) return Optional.empty();
            Usuario u = userOpt.get();
            if (u.getRol() != null && "admin".equalsIgnoreCase(u.getRol().getNombre())) return Optional.of(u);
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
