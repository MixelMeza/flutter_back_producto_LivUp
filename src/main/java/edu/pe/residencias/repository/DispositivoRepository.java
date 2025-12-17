package edu.pe.residencias.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.pe.residencias.model.entity.Dispositivo;

@Repository
public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {
    Optional<Dispositivo> findByFcmToken(String fcmToken);
    java.util.List<Dispositivo> findByUsuarioIdAndActivoTrue(Long usuarioId);
    java.util.List<Dispositivo> findAllByActivoTrue();
    void deleteByFcmToken(String fcmToken);
}
