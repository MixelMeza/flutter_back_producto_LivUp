package edu.pe.residencias.service.impl;

import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.pe.residencias.model.entity.Backup;
import edu.pe.residencias.model.entity.Usuario;
import edu.pe.residencias.repository.BackupRepository;
import edu.pe.residencias.service.BackupService;

@Service
public class BackupServiceImpl implements BackupService {

    @Autowired
    private BackupRepository backupRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public Backup createBackup(String name, Usuario createdBy) throws Exception {
        // Create logical DB dump: map table -> list of rows
        Map<String, Object> dump = new HashMap<>();

        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery("SHOW TABLES");
            while (rs.next()) {
                String table = rs.getString(1);
                try {
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM `" + table + "`");
                    dump.put(table, rows);
                } catch (Exception ex) {
                    // If a table can't be read, include an error message instead
                    dump.put(table, Map.of("error", ex.getMessage()));
                }
            }
        }

        byte[] json = objectMapper.writeValueAsBytes(dump);
        // compress
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(baos)) {
            gz.write(json);
        }
        byte[] compressed = baos.toByteArray();

        Backup b = new Backup();
        b.setName(name == null || name.isBlank() ? "backup-" + LocalDateTime.now() : name);
        b.setCreatedAt(LocalDateTime.now());
        b.setCreatedBy(createdBy);
        b.setContent(compressed);
        b.setSizeBytes((long) compressed.length);
        b.setMimeType("application/gzip");

        return backupRepository.save(b);
    }

    @Override
    public List<Backup> listBackups() {
        return backupRepository.findAll();
    }

    @Override
    public Optional<Backup> findById(Long id) {
        return backupRepository.findById(id);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        backupRepository.deleteById(id);
    }
}
