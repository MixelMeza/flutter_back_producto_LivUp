package edu.pe.residencias.service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

@Service
public class AppStatusService {

    private final AtomicBoolean maintenance = new AtomicBoolean(false);
    private volatile String message = "";

    public boolean isMaintenance() {
        return maintenance.get();
    }

    public String getMessage() {
        return message == null ? "" : message;
    }

    public void setMaintenance(boolean on, String msg) {
        maintenance.set(on);
        this.message = msg == null ? "" : msg;
    }

    public void clear() {
        maintenance.set(false);
        this.message = "";
    }

    public Map<String, Object> getStatus() {
        return Map.of("maintenance", isMaintenance(), "message", getMessage());
    }
}
