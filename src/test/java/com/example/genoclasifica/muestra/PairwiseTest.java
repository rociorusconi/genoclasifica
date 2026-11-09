package com.example.genoclasifica.muestra;

import com.example.genoclasifica.Categoria;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Pairwise (sección 4.4): 9 casos que cubren los 45 pares de tipo, categoría, confianza y estado de la muestra.
@SpringBootTest
@Transactional
class PairwiseTest {

    @Autowired
    private MuestraService service;

    @ParameterizedTest(name = "{0}: {1} {2} {3}% en muestra {4}")
    @CsvSource({
        "CP-PW-01, SNV,       BENIGNA,              50, ACEPTADA,             CLASIFICADA,          ,                     false",
        "CP-PW-02, Deleción,  SIGNIFICADO_INCIERTO, 90, ACEPTADA,             CLASIFICADA,          SIGNIFICADO_INCIERTO, false",
        "CP-PW-03, Inserción, PATOGENICA,           90, ACEPTADA,             PENDIENTE_FIRMA,      PATOGENICA,           true",
        "CP-PW-04, Inserción, BENIGNA,              90, CALIDAD_INSUFICIENTE, CALIDAD_INSUFICIENTE, ,                     false",
        "CP-PW-05, SNV,       SIGNIFICADO_INCIERTO, 50, CALIDAD_INSUFICIENTE, CALIDAD_INSUFICIENTE, ,                     false",
        "CP-PW-06, Deleción,  PATOGENICA,           50, CALIDAD_INSUFICIENTE, CALIDAD_INSUFICIENTE, ,                     false",
        "CP-PW-07, Deleción,  BENIGNA,              50, BLOQUEADA,            BLOQUEADA,            ,                     false",
        "CP-PW-08, Inserción, SIGNIFICADO_INCIERTO, 50, BLOQUEADA,            BLOQUEADA,            ,                     false",
        "CP-PW-09, SNV,       PATOGENICA,           90, BLOQUEADA,            BLOQUEADA,            ,                     false"
    })
    void combinacionDePares(String id, String tipo, Categoria categoria, double confianza, String estadoDeEntrada,
                            EstadoMuestra estadoEsperado, Categoria categoriaEsperada, boolean exigeFirma) {
        double q30 = estadoDeEntrada.equals("CALIDAD_INSUFICIENTE") ? 75.0 : 92.0;
        double contaminacion = estadoDeEntrada.equals("BLOQUEADA") ? 3.5 : 1.0;
        String contenido = "corrida " + id;

        service.recibir(new SolicitudRecepcion(id, "RUN-" + id, MuestraService.sha256(contenido), contenido, q30, contaminacion,
            List.of(new SolicitudRecepcion.VarianteRecibida("BRCA2", 32340300, tipo, categoria, confianza))));
        Muestra muestra = service.procesar(id);
        Variante variante = muestra.getVariantes().get(0);

        assertEquals(estadoEsperado, muestra.getEstado());
        assertEquals(categoriaEsperada, variante.getCategoria());
        assertEquals(exigeFirma, variante.isExigeFirma());
        if (estadoEsperado == EstadoMuestra.BLOQUEADA) {
            assertThrows(TransicionInvalidaException.class, () -> service.emitirInforme(id));
        }
    }
}
