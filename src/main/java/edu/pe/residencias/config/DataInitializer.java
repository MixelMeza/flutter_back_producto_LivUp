package edu.pe.residencias.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import edu.pe.residencias.model.entity.Persona;
import edu.pe.residencias.model.entity.Rol;
import edu.pe.residencias.model.entity.Usuario;
import edu.pe.residencias.repository.PersonaRepository;
import edu.pe.residencias.repository.RolRepository;
import edu.pe.residencias.repository.UsuarioRepository;

@Component
@Profile("!prod")
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RolRepository rolRepository, PersonaRepository personaRepository,
            UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.personaRepository = personaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // create base roles if none exist
        if (rolRepository.count() == 0) {
            Rol r1 = new Rol();
            r1.setNombre("propietario");
            r1.setDescripcion("Ofrece/gestiona residencias");
            rolRepository.save(r1);

            Rol r2 = new Rol();
            r2.setNombre("inquilino");
            r2.setDescripcion("Persona que alquila habitaciones");
            rolRepository.save(r2);

            Rol r3 = new Rol();
            r3.setNombre("admin");
            r3.setDescripcion("Administrador del sistema");
            rolRepository.save(r3);
            System.out.println("[DataInitializer] Roles created: propietario, inquilino, admin");
        }

        // ensure there is at least one admin user
        boolean hasAdmin = rolRepository.findByNombre("admin").map(r ->
            usuarioRepository.findAll().stream().anyMatch(u -> u.getRol() != null && u.getRol().getNombre().equals("admin"))
        ).orElse(false);

        if (!hasAdmin) {
            // create or reuse Persona for admin
            String adminDni = "99999999";
            String adminEmail = "admin.mixel@example.com";
            Persona p = personaRepository.findByDni(adminDni)
                    .or(() -> personaRepository.findByEmail(adminEmail))
                    .orElseGet(() -> {
                        Persona px = new Persona();
                        px.setNombre("Mixel");
                        px.setApellido("Meza");
                        px.setTipoDocumento("DNI");
                        px.setDni(adminDni);
                        px.setFechaNacimiento(null);
                        px.setTelefono("+51900000000");
                        px.setEmail(adminEmail);
                        px.setDireccion("Direccion admin");
                        px.setNotas(null);
                        px.setSexo(null);
                        px.setTelefonoApoderado(null);
                        px.setCreatedAt(LocalDateTime.now());
                        return personaRepository.save(px);
                    });

            // create Usuario admin if username not present
            String username = "adminmixel";
            boolean userExists = usuarioRepository.findByUsername(username).isPresent();
            if (!userExists) {
                Usuario u = new Usuario();
                u.setPersona(p);
                String rawPassword = "mixelMeza123"; // generated secure password
                u.setUsername(username);
                u.setPassword(passwordEncoder.encode(rawPassword));
                u.setEmailVerificado(true);
                u.setEstado("activo");
                u.setCreatedAt(LocalDateTime.now());
                // set role admin
                Rol adminRol = rolRepository.findByNombre("admin").orElse(null);
                u.setRol(adminRol);
                usuarioRepository.save(u);

                System.out.println("[DataInitializer] Admin user created:");
                System.out.println("  username: " + username);
                System.out.println("  password (plaintext for first-use): " + rawPassword);
                System.out.println("  NOTE: the password is stored encrypted (BCrypt) in the database.");
            } else {
                System.out.println("[DataInitializer] Admin username already exists: " + username);
            }
        }
    }
}