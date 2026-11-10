package com.example.genoclasifica.informe;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Informe emitido: el PDF tal como se entregó, su hash y las versiones con que se clasificó (RF-04, RF-07)
@Entity
@Table(name = "INFORME")
public class Informe {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String codigoMuestra;

    @Column(updatable = false)
    private String idCorrida;

    @Lob
    @Column(nullable = false, updatable = false)
    private byte[] contenido;

    @Column(nullable = false, updatable = false, length = 64)
    private String hashSha256;

    @Column(nullable = false, updatable = false)
    private LocalDateTime emitidoEn;

    @Column(updatable = false)
    private String versionGenoma;

    @Column(updatable = false)
    private String versionBases;

    @Column(updatable = false)
    private String versionModelo;

    protected Informe() {
    }

    Informe(String codigoMuestra, String idCorrida, byte[] contenido, String hashSha256,
            String versionGenoma, String versionBases, String versionModelo) {
        this.codigoMuestra = codigoMuestra;
        this.idCorrida = idCorrida;
        this.contenido = contenido;
        this.hashSha256 = hashSha256;
        this.emitidoEn = LocalDateTime.now();
        this.versionGenoma = versionGenoma;
        this.versionBases = versionBases;
        this.versionModelo = versionModelo;
    }

    public Long getId() { return id; }
    public String getCodigoMuestra() { return codigoMuestra; }
    public String getIdCorrida() { return idCorrida; }
    public byte[] getContenido() { return contenido.clone(); }
    public String getHashSha256() { return hashSha256; }
    public LocalDateTime getEmitidoEn() { return emitidoEn; }
    public String getVersionGenoma() { return versionGenoma; }
    public String getVersionBases() { return versionBases; }
    public String getVersionModelo() { return versionModelo; }
}
