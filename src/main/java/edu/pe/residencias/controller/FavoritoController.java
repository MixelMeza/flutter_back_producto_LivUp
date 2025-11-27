package edu.pe.residencias.controller;

import edu.pe.residencias.model.entity.Favorito;
import edu.pe.residencias.service.FavoritoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import edu.pe.residencias.security.JwtUtil;
import edu.pe.residencias.repository.UsuarioRepository;
import edu.pe.residencias.repository.FavoritoRepository;
import edu.pe.residencias.model.dto.FavoritoDTO;
import java.util.ArrayList;
import java.util.Comparator;

@RestController
@RequestMapping("/api/favoritos")
public class FavoritoController {

    @Autowired
    private FavoritoService favoritoService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private FavoritoRepository favoritoRepository;

    @GetMapping("/mine")
    public ResponseEntity<?> getMisFavoritos(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Falta Authorization header");
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            var claims = jwtUtil.parseToken(token);
            String uid = claims.get("uid", String.class);
            if (uid == null || uid.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido: uid faltante");
            }
            var usuarioOpt = usuarioRepository.findByUuid(uid);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
            }
            var usuario = usuarioOpt.get();
            
            List<Favorito> favoritos = favoritoRepository.findByUsuarioIdWithHabitacionAndResidencia(usuario.getId());
            
            if (favoritos.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            
            List<FavoritoDTO> dtos = new ArrayList<>();
            for (var fav : favoritos) {
                FavoritoDTO dto = new FavoritoDTO();
                dto.setFavoritoId(fav.getId());
                dto.setFechaAgregado(fav.getFecha() != null ? fav.getFecha().toString() : null);
                
                var hab = fav.getHabitacion();
                if (hab != null) {
                    dto.setHabitacionId(hab.getId());
                    dto.setHabitacionNumero(hab.getCodigoHabitacion());
                    dto.setHabitacionTipo(hab.getNombre());
                    dto.setHabitacionPrecio(hab.getPrecioMensual());
                    
                    var res = hab.getResidencia();
                    if (res != null) {
                        dto.setResidenciaId(res.getId());
                        dto.setResidenciaNombre(res.getNombre());
                        
                        var ub = res.getUbicacion();
                        if (ub != null) {
                            dto.setResidenciaDireccion(ub.getDireccion());
                        }
                        
                        // Get first image from habitacion
                        if (hab.getImagenesHabitacion() != null && !hab.getImagenesHabitacion().isEmpty()) {
                            String firstImg = hab.getImagenesHabitacion().stream()
                                .filter(img -> img != null)
                                .sorted(Comparator.comparing(img -> img.getOrden() == null ? Integer.MAX_VALUE : img.getOrden()))
                                .map(img -> img.getUrl())
                                .findFirst()
                                .orElse(null);
                            dto.setImagenUrl(firstImg);
                        }
                    }
                }
                
                dtos.add(dto);
            }
            
            return new ResponseEntity<>(dtos, HttpStatus.OK);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido");
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error interno al obtener favoritos", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public List<Favorito> listarFavoritos() {
        return favoritoService.findAll();
    }

    @GetMapping("/{id}")
    public Favorito getFavoritoById(@PathVariable Long id) {
        return favoritoService.findById(id);
    }

    @PostMapping
    public Favorito crearFavorito(@RequestBody Favorito favorito) {
        return favoritoService.save(favorito);
    }

    @PutMapping("/{id}")
    public Favorito actualizarFavorito(@PathVariable Long id, @RequestBody Favorito favorito) {
        favorito.setId(id);
        return favoritoService.save(favorito);
    }

    @DeleteMapping("/{id}")
    public void eliminarFavorito(@PathVariable Long id) {
        favoritoService.deleteById(id);
    }
}
