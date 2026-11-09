package com.example.genoclasifica.bdd;

import com.example.genoclasifica.muestra.MuestraService;
import com.example.genoclasifica.muestra.Variante;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

// Pasos de umbral_confianza.feature (RF-05)
public class UmbralConfianzaSteps {

    @Autowired
    private MuestraService service;

    private String codigo;
    private Variante variante;
    private RuntimeException error;

    // Expresión regular para leer "69.9" con punto: con el idioma español Cucumber espera coma decimal
    @Dado("^una variante que el modelo propone como \"([^\"]*)\" con ([\\d.]+) % de confianza$")
    public void unaVariante(String categoria, String confianza) {
        codigo = service.recibir(DatosDePrueba.muestra(92.0, 1.0,
            DatosDePrueba.categoria(categoria), Double.parseDouble(confianza))).getCodigo();
    }

    @Cuando("el sistema clasifica la variante")
    public void clasifica() {
        try {
            variante = service.procesar(codigo).getVariantes().get(0);
        } catch (IllegalArgumentException e) {
            error = e;
        }
    }

    @Entonces("la variante queda en estado {string}")
    public void quedaEnEstado(String estado) {
        assertNull(error);
        assertEquals(estado, variante.isRequiereRevision() ? "requiere revisión" : "clasificada");
    }

    @Y("la variante no tiene categoría asignada")
    public void sinCategoria() {
        assertNull(variante.getCategoria());
    }

    @Entonces("la clasificación se rechaza por confianza inválida")
    public void rechazada() {
        assertInstanceOf(IllegalArgumentException.class, error);
    }
}
