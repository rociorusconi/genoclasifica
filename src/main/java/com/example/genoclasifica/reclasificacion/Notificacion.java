package com.example.genoclasifica.reclasificacion;

import com.example.genoclasifica.Categoria;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// Aviso al genetista de que una variante de un informe ya emitido cambiaría de categoría (RF-08)
@Entity
@Table(name = "NOTIFICACION")
public class Notificacion {

    @Id
    @GeneratedValue
    private Long id;

    private String codigoMuestra;
    private String gen;
    private Integer posicion;

    @Enumerated(EnumType.STRING)
    private Categoria categoriaAnterior;

    @Enumerated(EnumType.STRING)
    private Categoria categoriaNueva;

    private String versionBases;
    private String destinatario;
    private LocalDateTime creadaEn;

    protected Notificacion() {
    }

    Notificacion(String codigoMuestra, String gen, Integer posicion, Categoria categoriaAnterior,
                 Categoria categoriaNueva, String versionBases) {
        this.codigoMuestra = codigoMuestra;
        this.gen = gen;
        this.posicion = posicion;
        this.categoriaAnterior = categoriaAnterior;
        this.categoriaNueva = categoriaNueva;
        this.versionBases = versionBases;
        this.destinatario = "GENETISTA";
        this.creadaEn = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getCodigoMuestra() { return codigoMuestra; }
    public String getGen() { return gen; }
    public Integer getPosicion() { return posicion; }
    public Categoria getCategoriaAnterior() { return categoriaAnterior; }
    public Categoria getCategoriaNueva() { return categoriaNueva; }
    public String getVersionBases() { return versionBases; }
    public String getDestinatario() { return destinatario; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
}
