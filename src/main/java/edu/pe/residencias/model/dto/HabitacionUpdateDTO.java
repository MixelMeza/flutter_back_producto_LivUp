package edu.pe.residencias.model.dto;

import java.math.BigDecimal;

public class HabitacionUpdateDTO {
    private String codigoHabitacion;
    private String nombre;
    private Boolean departamento;
    private Boolean banoPrivado;
    private Boolean wifi;
    private Boolean amueblado;
    private Integer piso;
    private Integer capacidad;
    private String descripcion;
    private Boolean permitir_mascotas;
    private Boolean agua;
    private Boolean luz;
    private BigDecimal precioMensual;
    private String estado;
    private Boolean destacado;
    private Boolean terma;

    public HabitacionUpdateDTO() {}

    public String getCodigoHabitacion() { return codigoHabitacion; }
    public void setCodigoHabitacion(String codigoHabitacion) { this.codigoHabitacion = codigoHabitacion; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Boolean getDepartamento() { return departamento; }
    public void setDepartamento(Boolean departamento) { this.departamento = departamento; }
    public Boolean getBanoPrivado() { return banoPrivado; }
    public void setBanoPrivado(Boolean banoPrivado) { this.banoPrivado = banoPrivado; }
    public Boolean getWifi() { return wifi; }
    public void setWifi(Boolean wifi) { this.wifi = wifi; }
    public Boolean getAmueblado() { return amueblado; }
    public void setAmueblado(Boolean amueblado) { this.amueblado = amueblado; }
    public Integer getPiso() { return piso; }
    public void setPiso(Integer piso) { this.piso = piso; }
    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Boolean getPermitir_mascotas() { return permitir_mascotas; }
    public void setPermitir_mascotas(Boolean permitir_mascotas) { this.permitir_mascotas = permitir_mascotas; }
    public Boolean getAgua() { return agua; }
    public void setAgua(Boolean agua) { this.agua = agua; }
    public Boolean getLuz() { return luz; }
    public void setLuz(Boolean luz) { this.luz = luz; }
    public BigDecimal getPrecioMensual() { return precioMensual; }
    public void setPrecioMensual(BigDecimal precioMensual) { this.precioMensual = precioMensual; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Boolean getDestacado() { return destacado; }
    public void setDestacado(Boolean destacado) { this.destacado = destacado; }
    public Boolean getTerma() { return terma; }
    public void setTerma(Boolean terma) { this.terma = terma; }
}
