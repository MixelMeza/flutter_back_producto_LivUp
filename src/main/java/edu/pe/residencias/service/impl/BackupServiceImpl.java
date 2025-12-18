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
        b.setName(name == null || name.isBlank() ? "backup-" + edu.pe.residencias.util.DateTimeUtil.nowLima() : name);
        b.setCreatedAt(edu.pe.residencias.util.DateTimeUtil.nowLima());
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
    public java.util.Optional<Backup> findMostRecent() {
        return backupRepository.findFirstByOrderByCreatedAtDesc();
    }

    @Override
    public java.util.Optional<Backup> findLatestByDate(java.time.LocalDate date) {
        if (date == null) return java.util.Optional.empty();
        java.time.LocalDateTime start = date.atStartOfDay();
        java.time.LocalDateTime end = start.plusDays(1);
        return backupRepository.findFirstByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
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

    @Override
    @Transactional
    public Backup updateName(Long id, String newName) throws Exception {
        var opt = backupRepository.findById(id);
        if (opt.isEmpty()) throw new IllegalArgumentException("Backup not found");
        Backup b = opt.get();
        b.setName(newName == null || newName.isBlank() ? b.getName() : newName);
        return backupRepository.save(b);
    }

    @Override
    @Transactional
    public Backup saveUploadedBackup(String name, byte[] content, String mimeType, Usuario createdBy) throws Exception {
        if (content == null) throw new IllegalArgumentException("Content is null");
        Backup b = new Backup();
        b.setName(name == null || name.isBlank() ? "uploaded-backup-" + edu.pe.residencias.util.DateTimeUtil.nowLima() : name);
        b.setCreatedAt(edu.pe.residencias.util.DateTimeUtil.nowLima());
        b.setCreatedBy(createdBy);
        b.setContent(content);
        b.setSizeBytes((long) content.length);
        b.setMimeType(mimeType == null ? "application/gzip" : mimeType);
        return backupRepository.save(b);
    }

    @Override
    @Transactional
    public void restoreBackup(Long id, boolean truncate) throws Exception {
        var opt = backupRepository.findById(id);
        if (opt.isEmpty()) throw new IllegalArgumentException("Backup not found");
        Backup b = opt.get();
        if (b.getContent() == null || b.getContent().length == 0) throw new IllegalStateException("Backup content empty");

        // decompress
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(b.getContent());
        java.util.zip.GZIPInputStream gis = new java.util.zip.GZIPInputStream(bais);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = gis.read(buf)) != -1) baos.write(buf, 0, r);
        gis.close();
        byte[] json = baos.toByteArray();

        // parse JSON to Map<table, List<rowMap>>
        @SuppressWarnings("unchecked")
        Map<String, Object> dump = objectMapper.readValue(json, Map.class);

        // For each table, optionally truncate and insert rows
        for (Map.Entry<String, Object> e : dump.entrySet()) {
            String table = e.getKey();
            Object value = e.getValue();
            if (!(value instanceof List)) {
                // skip non-list entries (errors etc.)
                continue;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) value;

            if (truncate) {
                // delete existing rows
                jdbcTemplate.execute("DELETE FROM `" + table + "`");
            }

            // insert rows
            for (Map<String, Object> row : rows) {
                if (row == null) continue;
                var cols = new java.util.ArrayList<String>();
                var vals = new java.util.ArrayList<Object>();
                for (Map.Entry<String, Object> colEntry : row.entrySet()) {
                    cols.add("`" + colEntry.getKey() + "`");
                    vals.add(colEntry.getValue());
                }
                if (cols.isEmpty()) continue;
                String sql = "INSERT INTO `" + table + "` (" + String.join(",", cols) + ") VALUES (" + String.join(",", java.util.Collections.nCopies(cols.size(), "?")) + ")";
                try {
                    jdbcTemplate.update(sql, vals.toArray());
                } catch (Exception ex) {
                    // if insert fails, throw to abort restore
                    throw new RuntimeException("Failed to insert into table " + table + ": " + ex.getMessage(), ex);
                }
            }
        }
    }
}
