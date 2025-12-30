package edu.pe.residencias.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pe.residencias.model.dto.HabitacionStatsCardDTO;
import edu.pe.residencias.model.entity.Habitacion;
import edu.pe.residencias.model.entity.ImagenHabitacion;
import edu.pe.residencias.repository.FavoritoRepository;
import edu.pe.residencias.repository.HabitacionRepository;
import edu.pe.residencias.repository.ImagenHabitacionRepository;
import edu.pe.residencias.repository.UsuarioRepository;
import edu.pe.residencias.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/stats/habitaciones")
public class StatsController {

	@Autowired
	private HabitacionRepository habitacionRepository;

	@Autowired
	private ImagenHabitacionRepository imagenHabitacionRepository;

	@Autowired
	private FavoritoRepository favoritoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private JwtUtil jwtUtil;

	// Un solo endpoint: envías el id y devuelve el DTO compacto.
	@GetMapping("/{habitacionId}/card")
	@Transactional(readOnly = true)
	public ResponseEntity<HabitacionStatsCardDTO> getHabitacionStatsCard(
			HttpServletRequest request,
			@PathVariable("habitacionId") Long habitacionId) {
		try {
			if (habitacionId == null) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			Habitacion h = habitacionRepository.findById(habitacionId).orElse(null);
			if (h == null) {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}

			Long usuarioId = resolveUsuarioIdFromTokenIfPresent(request);

			HabitacionStatsCardDTO dto = new HabitacionStatsCardDTO();
			dto.setHabitacionId(h.getId());
			dto.setNombreHabitacion(h.getNombre());
			dto.setDepartamento(h.getDepartamento());
			dto.setPrecioMensual(h.getPrecioMensual());

			if (h.getResidencia() != null) {
				dto.setNombreResidencia(h.getResidencia().getNombre());
				if (h.getResidencia().getUbicacion() != null) {
					dto.setLat(h.getResidencia().getUbicacion().getLatitud());
					dto.setLng(h.getResidencia().getUbicacion().getLongitud());
				}
			}

			List<ImagenHabitacion> imagenes = imagenHabitacionRepository.findByHabitacionId(habitacionId);
			dto.setImagenPrincipalId(selectImagenPrincipalId(imagenes));

			boolean liked = false;
			if (usuarioId != null) {
				liked = favoritoRepository.existsByUsuarioIdAndHabitacionId(usuarioId, habitacionId);
			}
			dto.setLiked(liked);

			return new ResponseEntity<>(dto, HttpStatus.OK);
		} catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		} catch (Exception ex) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private Long resolveUsuarioIdFromTokenIfPresent(HttpServletRequest request) {
		if (request == null) return null;
		String authHeader = request.getHeader("Authorization");
		if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
			return null;
		}
		String token = authHeader.substring("Bearer ".length()).trim();
		if (token.isBlank()) return null;

		io.jsonwebtoken.Claims claims = jwtUtil.parseToken(token);
		String uid = claims.get("uid", String.class);
		if (uid == null || uid.isBlank()) throw new IllegalArgumentException("Token inválido: uid faltante");

		var usuarioOpt = usuarioRepository.findByUuid(uid);
		if (usuarioOpt.isEmpty()) throw new IllegalArgumentException("Usuario no encontrado");
		return usuarioOpt.get().getId();
	}

	private Long selectImagenPrincipalId(List<ImagenHabitacion> imagenes) {
		if (imagenes == null || imagenes.isEmpty()) return null;

		return imagenes.stream()
				.filter(ih -> ih != null && ih.getId() != null)
				.filter(ih -> ih.getEstado() == null || ih.getEstado().isBlank() || "activo".equalsIgnoreCase(ih.getEstado()))
				.min(Comparator
						.comparing((ImagenHabitacion ih) -> ih.getOrden() == null ? Integer.MAX_VALUE : ih.getOrden())
						.thenComparing(ih -> ih.getId() == null ? Long.MAX_VALUE : ih.getId()))
				.map(ImagenHabitacion::getId)
				.orElse(null);
	}
}
