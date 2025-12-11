package edu.pe.residencias.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class BackupManagementService {

    private final Path backupsRoot = Paths.get("backups");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String, Object>> listBackups() throws IOException {
        if (!Files.exists(backupsRoot)) return List.of();
        try (var stream = Files.list(backupsRoot)) {
            java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
            stream.filter(Files::isDirectory).forEach(p -> {
                try {
                    BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
                    long size = Files.walk(p).filter(Files::isRegularFile).mapToLong(f -> {
                        try { return Files.size(f); } catch (Exception e) { return 0L; }
                    }).sum();
                    long files = Files.walk(p).filter(Files::isRegularFile).count();
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", p.getFileName().toString());
                    m.put("createdAt", attr.creationTime().toString());
                    m.put("modifiedAt", attr.lastModifiedTime().toString());
                    m.put("fileCount", files);
                    m.put("sizeBytes", size);
                    result.add(m);
                } catch (IOException ex) {
                    result.add(Map.of("name", p.getFileName().toString(), "error", ex.getMessage()));
                }
            });
            return result.stream().sorted(Comparator.comparing(m -> ((String)m.get("name")), Comparator.reverseOrder())).collect(Collectors.toList());
        }
    }

    public boolean deleteBackup(String name) throws IOException {
        Path folder = backupsRoot.resolve(name);
        if (!Files.exists(folder) || !Files.isDirectory(folder)) return false;
        // delete recursively
        Files.walk(folder).sorted(Comparator.reverseOrder()).forEach(p -> {
            try { Files.deleteIfExists(p); } catch (IOException e) { /* ignore */ }
        });
        return true;
    }

    public void downloadBackup(String name, OutputStream out) throws IOException {
        Path folder = backupsRoot.resolve(name);
        if (!Files.exists(folder) || !Files.isDirectory(folder)) throw new IOException("Backup not found");
        try (var zip = new java.util.zip.ZipOutputStream(out)) {
            Files.walk(folder).filter(Files::isRegularFile).forEach(p -> {
                try {
                    String rel = folder.relativize(p).toString().replace('\\', '/');
                    zip.putNextEntry(new java.util.zip.ZipEntry(rel));
                    Files.copy(p, zip);
                    zip.closeEntry();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            zip.finish();
        }
    }

    private static final String[] DEFAULT_ORDER = new String[] {
        "roles", "persona", "usuarios", "residencias", "habitaciones",
        "imagen_residencia", "imagen_habitacion", "contratos", "pagos", "abonos",
        "reviews", "notificaciones", "accesos", "device_tokens", "favoritos", "gastos_residencia"
    };

    public void restoreBackup(String name, boolean truncate) throws IOException {
        Path folder = backupsRoot.resolve(name);
        if (!Files.exists(folder) || !Files.isDirectory(folder)) throw new IOException("Backup folder not found");

        Map<String, Path> files = new HashMap<>();
        try (var stream = Files.list(folder)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(p -> files.put(stripExt(p.getFileName().toString()), p));
        }

        List<String> order = new ArrayList<>();
        for (String s : DEFAULT_ORDER) if (files.containsKey(s)) order.add(s);
        for (String s : files.keySet()) if (!order.contains(s)) order.add(s);

        // disable FK checks
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        try {
            for (String table : order) {
                Path p = files.get(table);
                if (p == null) continue;
                List<Map<String, Object>> rows = objectMapper.readValue(p.toFile(), new TypeReference<List<Map<String,Object>>>(){});
                if (rows.isEmpty()) continue;
                List<String> cols = new ArrayList<>(rows.get(0).keySet());
                String colsSql = cols.stream().map(c -> "`"+c+"`").collect(Collectors.joining(","));
                String placeholders = cols.stream().map(c -> "?").collect(Collectors.joining(","));
                if (truncate) jdbcTemplate.execute("TRUNCATE TABLE `"+table+"`");
                String sql = "INSERT INTO `"+table+"` ("+colsSql+") VALUES ("+placeholders+")";
                List<Object[]> batch = new ArrayList<>();
                for (Map<String,Object> r : rows) {
                    Object[] vals = cols.stream().map(c -> r.get(c)).toArray();
                    batch.add(vals);
                }
                jdbcTemplate.batchUpdate(sql, batch);
            }
        } finally {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
        }
    }

    private String stripExt(String n) { return n.replaceFirst("\\.json$", ""); }
}
