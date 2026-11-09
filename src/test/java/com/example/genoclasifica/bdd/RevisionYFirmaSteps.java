package com.example.genoclasifica.bdd;

import com.example.genoclasifica.Categoria;
import com.example.genoclasifica.muestra.MuestraService;
import com.example.genoclasifica.muestra.PermisoDenegadoException;
import com.example.genoclasifica.muestra.TransicionInvalidaException;
import com.example.genoclasifica.seguridad.Rol;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dada;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

// Pasos de revision_y_firma.feature (RF-06, RF-07)
public class RevisionYFirmaSteps {

    @Autowired
    private MuestraService service;

    private String codigo;
    private boolean informeEmitido;
    private RuntimeException error;

    @Dada("una muestra aceptada con una variante {string}")
    public void muestraAceptada(String categoria) {
        prepararMuestra(1.0, DatosDePrueba.categoria(categoria));
    }

    @Dada("una muestra bloqueada por contaminación")
    public void muestraBloqueada() {
        prepararMuestra(3.5, Categoria.PATOGENICA);
    }

    @Y("el genetista firma la muestra")
    public void genetistaFirma() {
        service.firmar(codigo, "genetista_1", Rol.GENETISTA);
    }

    @Cuando("se pide el informe de la muestra")
    public void pideInforme() {
        try {
            service.emitirInforme(codigo);
            informeEmitido = true;
        } catch (TransicionInvalidaException e) {
            error = e;
        }
    }

    @Cuando("un usuario con rol {string} intenta firmar la muestra")
    public void intentaFirmar(String rol) {
        try {
            service.firmar(codigo, "usuario_" + rol, DatosDePrueba.rol(rol));
        } catch (PermisoDenegadoException e) {
            error = e;
        }
    }

    @Entonces("el informe se emite")
    public void seEmite() {
        assertTrue(informeEmitido);
    }

    @Entonces("el informe no se emite")
    public void noSeEmite() {
        assertFalse(informeEmitido);
        assertInstanceOf(TransicionInvalidaException.class, error);
    }

    @Entonces("la firma se rechaza por falta de permisos")
    public void firmaRechazada() {
        assertInstanceOf(PermisoDenegadoException.class, error);
    }

    // El estado se vuelve a leer de la base: se verifica lo que quedó guardado, no un mensaje
    @Y("la muestra sigue en estado {string}")
    public void sigueEnEstado(String estado) {
        assertEquals(estado, service.buscar(codigo).getEstado().getDescripcion());
    }

    @Y("la muestra queda en estado {string}")
    public void quedaEnEstado(String estado) {
        sigueEnEstado(estado);
    }

    private void prepararMuestra(double contaminacion, Categoria categoria) {
        codigo = service.recibir(DatosDePrueba.muestra(92.0, contaminacion, categoria, 90.0)).getCodigo();
        service.procesar(codigo);
    }
}
