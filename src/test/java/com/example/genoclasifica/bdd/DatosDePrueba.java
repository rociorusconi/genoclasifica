package com.example.genoclasifica.bdd;

import com.example.genoclasifica.Categoria;
import com.example.genoclasifica.muestra.SolicitudRecepcion;
import com.example.genoclasifica.seguridad.Rol;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

// Traduce el lenguaje de los escenarios a datos sintéticos del sistema
final class DatosDePrueba {

    private static final AtomicInteger SECUENCIA = new AtomicInteger(1);

    private static final Map<String, Categoria> CATEGORIAS = Map.of(
        "benigna", Categoria.BENIGNA,
        "probablemente benigna", Categoria.PROBABLEMENTE_BENIGNA,
        "significado incierto", Categoria.SIGNIFICADO_INCIERTO,
        "probablemente patogénica", Categoria.PROBABLEMENTE_PATOGENICA,
        "patogénica", Categoria.PATOGENICA);

    private static final Map<String, Rol> ROLES = Map.of(
        "técnico", Rol.TECNICO,
        "bioinformático", Rol.BIOINFORMATICO,
        "genetista", Rol.GENETISTA,
        "administrador", Rol.ADMINISTRADOR,
        "auditor", Rol.AUDITOR);

    private DatosDePrueba() {
    }

    static Categoria categoria(String texto) {
        return CATEGORIAS.get(texto);
    }

    static Rol rol(String texto) {
        return ROLES.get(texto);
    }

    // Cada escenario usa un código y una corrida nuevos para no chocar con los anteriores
    static SolicitudRecepcion muestra(double q30, double contaminacion, Categoria categoria, double confianza) {
        String codigo = "BDD-" + SECUENCIA.getAndIncrement();
        String contenido = "corrida sintetica " + codigo;
        return new SolicitudRecepcion(codigo, "RUN-" + codigo, sha256(contenido), contenido, q30, contaminacion,
            List.of(new SolicitudRecepcion.VarianteRecibida("BRCA1", 43071077, "SNV", categoria, confianza)));
    }

    private static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
