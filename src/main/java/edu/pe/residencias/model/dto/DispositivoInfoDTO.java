package edu.pe.residencias.model.dto;

import java.time.LocalDateTime;

public class DispositivoInfoDTO {
    private Long id;
    private String fcmToken;
    private String plataforma;
    private String modelo;
    private String nombre; // friendly name (plataforma + modelo)
    private Boolean activo;
    private LocalDateTime primeroVisto; // dispositivo.createdAt
    private LocalDateTime ultimoAcceso; // acceso.ultimaSesion
    private String ultimoTipo; // LOGIN or LOGOUT
    private Boolean estaLogueado; // true if ultimoTipo == LOGIN

    public DispositivoInfoDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public String getPlataforma() { return plataforma; }
    public void setPlataforma(String plataforma) { this.plataforma = plataforma; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public LocalDateTime getPrimeroVisto() { return primeroVisto; }
    public void setPrimeroVisto(LocalDateTime primeroVisto) { this.primeroVisto = primeroVisto; }

    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    public void setUltimoAcceso(LocalDateTime ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }

    public String getUltimoTipo() { return ultimoTipo; }
    public void setUltimoTipo(String ultimoTipo) { this.ultimoTipo = ultimoTipo; }

    public Boolean getEstaLogueado() { return estaLogueado; }
    public void setEstaLogueado(Boolean estaLogueado) { this.estaLogueado = estaLogueado; }
}
