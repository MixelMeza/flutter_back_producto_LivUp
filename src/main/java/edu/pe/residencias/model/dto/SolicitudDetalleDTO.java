package edu.pe.residencias.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import edu.pe.residencias.model.enums.SolicitudEstado;

public class SolicitudDetalleDTO {
    private Long id;
    private LocalDateTime fechaSolicitud;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Integer duracionMeses;
    private Boolean fijo;
    private SolicitudEstado estado;
    private String comentarios;

    private String habitacionNombre;
    private String residenciaNombre;

    private String estudianteNombreCompleto;
    private String estudianteFotoPerfil;
    private String estudianteEmail;
    private String estudianteTelefono;
    private String estudianteNotas;

    public SolicitudDetalleDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public Integer getDuracionMeses() { return duracionMeses; }
    public void setDuracionMeses(Integer duracionMeses) { this.duracionMeses = duracionMeses; }

    public Boolean getFijo() { return fijo; }
    public void setFijo(Boolean fijo) { this.fijo = fijo; }

    public SolicitudEstado getEstado() { return estado; }
    public void setEstado(SolicitudEstado estado) { this.estado = estado; }

    public String getComentarios() { return comentarios; }
    public void setComentarios(String comentarios) { this.comentarios = comentarios; }

    public String getHabitacionNombre() { return habitacionNombre; }
    public void setHabitacionNombre(String habitacionNombre) { this.habitacionNombre = habitacionNombre; }

    public String getResidenciaNombre() { return residenciaNombre; }
    public void setResidenciaNombre(String residenciaNombre) { this.residenciaNombre = residenciaNombre; }

    public String getEstudianteNombreCompleto() { return estudianteNombreCompleto; }
    public void setEstudianteNombreCompleto(String estudianteNombreCompleto) { this.estudianteNombreCompleto = estudianteNombreCompleto; }

    public String getEstudianteFotoPerfil() { return estudianteFotoPerfil; }
    public void setEstudianteFotoPerfil(String estudianteFotoPerfil) { this.estudianteFotoPerfil = estudianteFotoPerfil; }

    public String getEstudianteEmail() { return estudianteEmail; }
    public void setEstudianteEmail(String estudianteEmail) { this.estudianteEmail = estudianteEmail; }

    public String getEstudianteTelefono() { return estudianteTelefono; }
    public void setEstudianteTelefono(String estudianteTelefono) { this.estudianteTelefono = estudianteTelefono; }

    public String getEstudianteNotas() { return estudianteNotas; }
    public void setEstudianteNotas(String estudianteNotas) { this.estudianteNotas = estudianteNotas; }
}
