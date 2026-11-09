package com.example.genoclasifica.muestra;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Muestra {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo;

    private String idCorrida;
    private String checksum;
    private Double q30;
    private Double contaminacion;
    private LocalDate fechaRecepcion;

    @Enumerated(EnumType.STRING)
    private EstadoMuestra estado;

    private String firmadaPor;

    @OneToMany(mappedBy = "muestra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Variante> variantes = new ArrayList<>();

    protected Muestra() {
    }

    public Muestra(String codigo, String idCorrida, String checksum, Double q30, Double contaminacion) {
        this.codigo = codigo;
        this.idCorrida = idCorrida;
        this.checksum = checksum;
        this.q30 = q30;
        this.contaminacion = contaminacion;
        this.fechaRecepcion = LocalDate.now();
    }

    public void agregarVariante(Variante variante) {
        variante.asignarMuestra(this);
        variantes.add(variante);
    }

    public boolean tieneVariantesQueExigenFirma() {
        return variantes.stream().anyMatch(Variante::isExigeFirma);
    }

    void cambiarEstado(EstadoMuestra nuevoEstado) {
        this.estado = nuevoEstado;
    }

    void registrarFirma(String usuario) {
        this.firmadaPor = usuario;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getIdCorrida() { return idCorrida; }
    public String getChecksum() { return checksum; }
    public Double getQ30() { return q30; }
    public Double getContaminacion() { return contaminacion; }
    public LocalDate getFechaRecepcion() { return fechaRecepcion; }
    public EstadoMuestra getEstado() { return estado; }
    public String getFirmadaPor() { return firmadaPor; }
    public List<Variante> getVariantes() { return variantes; }
}
