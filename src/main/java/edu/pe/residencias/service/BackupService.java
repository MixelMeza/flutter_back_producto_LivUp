package edu.pe.residencias.service;

import java.util.List;
import java.util.Optional;

import edu.pe.residencias.model.entity.Backup;
import edu.pe.residencias.model.entity.Usuario;

public interface BackupService {
    Backup createBackup(String name, Usuario createdBy) throws Exception;
    List<Backup> listBackups();
    Optional<Backup> findById(Long id);
    void deleteById(Long id);
}
