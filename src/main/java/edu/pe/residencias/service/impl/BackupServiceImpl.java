package edu.pe.residencias.service.impl;

import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.zip.GZIPOutputStream;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.pe.residencias.model.entity.Backup;
import edu.pe.residencias.model.entity.Usuario;
import edu.pe.residencias.repository.BackupRepository;
import edu.pe.residencias.service.BackupService;

@Service
public class BackupServiceImpl implements BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupServiceImpl.class);

    @Autowired
    private BackupRepository backupRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    private final ObjectMapper objectMapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper m = new ObjectMapper();
        try {
            // Register JavaTime module so LocalDateTime and other java.time types are handled
            m.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            // Prefer ISO-8601 string representation instead of timestamps
            m.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        } catch (NoClassDefFoundError ex) {
            // jackson-datatype-jsr310 might not be on the classpath in some environments;
            // leave default mapper in that case and surface potential errors later.
        }
        return m;
    }

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

        java.sql.Connection conn = null;
        boolean isMySql = false;
        try {
            conn = dataSource.getConnection();
            String product = conn.getMetaData().getDatabaseProductName();
            if (product != null && product.toLowerCase().contains("mysql")) {
                isMySql = true;
            }
        } finally {
            if (conn != null) conn.close();
        }

        // If truncating and DB is MySQL, disable FK checks temporarily to avoid constraint errors
        if (truncate && isMySql) {
            try {
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
            } catch (Exception ex) {
                // ignore - best effort
            }
        }

        try {
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

            // get existing columns for target table to avoid inserting unknown columns
            Set<String> existingCols = getExistingTableColumns(table);

            // insert rows
            for (Map<String, Object> row : rows) {
                if (row == null) continue;
                var cols = new java.util.ArrayList<String>();
                var vals = new java.util.ArrayList<Object>();
                var ignored = new java.util.ArrayList<String>();
                for (Map.Entry<String, Object> colEntry : row.entrySet()) {
                    String colName = colEntry.getKey();
                    // If column does not exist, try to create it dynamically (best-effort)
                    if (!existingCols.contains(colName)) {
                        if (isSafeIdentifier(colName)) {
                            try {
                                addColumnIfNotExists(table, colName, colEntry.getValue());
                                // refresh existing columns set
                                existingCols = getExistingTableColumns(table);
                            } catch (Exception ex) {
                                // If we can't create the column, skip and log
                                ignored.add(colName);
                                logger.warn("Could not create missing column {} in table {}: {}", colName, table, ex.getMessage());
                                continue;
                            }
                        } else {
                            ignored.add(colName);
                            logger.warn("Skipping unsafe column name for table {}: {}", table, colName);
                            continue;
                        }
                    }
                    cols.add("`" + colName + "`");
                    vals.add(colEntry.getValue());
                }
                if (!ignored.isEmpty()) {
                    logger.warn("Skipping unknown columns for table {}: {}", table, String.join(",", ignored));
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

            // After inserting, if rows contained an 'id' column numeric, try to adjust AUTO_INCREMENT (MySQL)
            try {
                if (!rows.isEmpty() && rows.get(0).containsKey("id") && isMySql) {
                    long maxId = -1L;
                    for (Map<String, Object> row : rows) {
                        Object idVal = row.get("id");
                        if (idVal instanceof Number) {
                            long v = ((Number) idVal).longValue();
                            if (v > maxId) maxId = v;
                        } else if (idVal != null) {
                            try {
                                long v = Long.parseLong(idVal.toString());
                                if (v > maxId) maxId = v;
                            } catch (Exception ex) {
                                // ignore non-numeric id
                            }
                        }
                    }
                    if (maxId >= 0) {
                        try {
                            jdbcTemplate.execute("ALTER TABLE `" + table + "` AUTO_INCREMENT = " + (maxId + 1));
                        } catch (Exception ex) {
                            // ignore failures adjusting auto_increment
                        }
                    }
                }
            } catch (Exception ex) {
                // ignore auto-increment adjustments
            }
        }
        } finally {
            if (truncate && isMySql) {
                try {
                    jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
        }

    private Set<String> getExistingTableColumns(String table) {
        Set<String> cols = new HashSet<>();
        java.sql.Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement("SELECT * FROM `" + table + "` LIMIT 0");
            rs = ps.executeQuery();
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                cols.add(md.getColumnName(i));
            }
        } catch (SQLException ex) {
            // If table doesn't exist or metadata cannot be read, return empty set
            logger.warn("Could not read metadata for table {}: {}", table, ex.getMessage());
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
        return cols;
    }

    /**
     * Check that an identifier (table/column) contains only safe characters.
     * This is a defensive check before building DDL SQL strings.
     */
    private boolean isSafeIdentifier(String ident) {
        if (ident == null) return false;
        return ident.matches("[A-Za-z0-9_]+");
    }

    /**
     * Infer a reasonable SQL column type from a sample Java object.
     * This is a best-effort heuristic and may be adjusted for your schema.
     */
    private String inferSqlType(Object sample) {
        if (sample == null) return "TEXT";
        if (sample instanceof Integer || sample instanceof Long) return "BIGINT";
        if (sample instanceof Short || sample instanceof Byte) return "SMALLINT";
        if (sample instanceof Float || sample instanceof Double) return "DOUBLE";
        if (sample instanceof Boolean) return "TINYINT(1)";
        if (sample instanceof java.util.Map || sample instanceof java.util.List) {
            // If MySQL supports JSON, use it; otherwise fall back to TEXT
            return "JSON";
        }
        String s = sample.toString();
        // ISO datetime detection (e.g. 2025-12-18T16:01:15 or 2025-12-18 16:01:15)
        if (s.matches("\\d{4}-\\d{2}-\\d{2}T.*") || s.matches("\\d{4}-\\d{2}-\\d{2} .*")) {
            return "DATETIME";
        }
        if (s.length() > 1024) return "LONGTEXT";
        if (s.length() > 255) return "TEXT";
        return "VARCHAR(255)";
    }

    /**
     * Attempt to add a column to the target table if it does not already exist.
     * This is executed as a best-effort to prevent data loss when restoring backups
     * that contain columns not present in the destination schema.
     */
    private void addColumnIfNotExists(String table, String column, Object sample) {
        if (!isSafeIdentifier(table) || !isSafeIdentifier(column)) {
            throw new IllegalArgumentException("Unsafe table or column name");
        }
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=? AND column_name=?",
                    Integer.class, table, column);
            if (cnt != null && cnt > 0) return; // already exists
        } catch (Exception ex) {
            // If we can't query information_schema, fail fast to avoid blind ALTERs
            throw new RuntimeException("Could not verify existing columns for table " + table + ": " + ex.getMessage(), ex);
        }

        String sqlType = inferSqlType(sample);
        String alter = "ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + sqlType + " NULL";
        try {
            jdbcTemplate.execute(alter);
            logger.info("Added missing column {} to table {} as {}", column, table, sqlType);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to add column " + column + " to table " + table + ": " + ex.getMessage(), ex);
        }
    }
}