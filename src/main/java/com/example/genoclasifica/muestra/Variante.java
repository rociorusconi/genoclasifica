package com.example.genoclasifica.muestra;

import com.example.genoclasifica.Categoria;
import com.example.genoclasifica.ResultadoClasificacion;
import jakarta.persistence.*;

@Entity
public class Variante {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    private Muestra muestra;

    private String gen;
    private Integer posicion;
    private String tipo;

    // Lo que propone el modelo de IA (simulado)
    @Enumerated(EnumType.STRING)
    private Categoria categoriaPropuesta;
    private Double confianza;

    // Lo que queda después de aplicar el umbral de confianza
    @Enumerated(EnumType.STRING)
    private Categoria categoria;
    private boolean requiereRevision;
    private boolean exigeFirma;

    protected Variante() {
    }

    public Variante(String gen, Integer posicion, String tipo, Categoria categoriaPropuesta, Double confianza) {
        this.gen = gen;
        this.posicion = posicion;
        this.tipo = tipo;
        this.categoriaPropuesta = categoriaPropuesta;
        this.confianza = confianza;
    }

    void aplicar(ResultadoClasificacion resultado) {
        this.categoria = resultado.categoria();
        this.requiereRevision = resultado.requiereRevision();
        this.exigeFirma = resultado.exigeFirma();
    }

    void asignarMuestra(Muestra muestra) {
        this.muestra = muestra;
    }

    public Long getId() { return id; }
    public String getGen() { return gen; }
    public Integer getPosicion() { return posicion; }
    public String getTipo() { return tipo; }
    public Categoria getCategoriaPropuesta() { return categoriaPropuesta; }
    public Double getConfianza() { return confianza; }
    public Categoria getCategoria() { return categoria; }
    public boolean isRequiereRevision() { return requiereRevision; }
    public boolean isExigeFirma() { return exigeFirma; }
}
