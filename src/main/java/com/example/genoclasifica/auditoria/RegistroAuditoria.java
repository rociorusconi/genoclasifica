package com.example.genoclasifica.auditoria;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Una fila por acción. Sin setters: una vez guardada no se cambia (RF-10).
@Entity
@Table(name = "AUDITORIA")
public class RegistroAuditoria {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false, updatable = false)
    private String usuario;

    @Column(nullable = false, updatable = false)
    private String accion;

    @Column(updatable = false)
    private String codigoMuestra;

    @Column(length = 1000, updatable = false)
    private String detalle;

    protected RegistroAuditoria() {
    }

    RegistroAuditoria(String usuario, String accion, String codigoMuestra, String detalle) {
        this.fechaHora = LocalDateTime.now();
        this.usuario = usuario;
        this.accion = accion;
        this.codigoMuestra = codigoMuestra;
        this.detalle = detalle;
    }

    public Long getId() { return id; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getUsuario() { return usuario; }
    public String getAccion() { return accion; }
    public String getCodigoMuestra() { return codigoMuestra; }
    public String getDetalle() { return detalle; }
}
