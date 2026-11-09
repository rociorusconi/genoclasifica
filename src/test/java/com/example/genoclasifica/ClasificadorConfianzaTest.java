package com.example.genoclasifica;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Umbral de confianza (RF-05): bajo 70 % la variante "requiere revisión" y nunca queda como benigna.
class ClasificadorConfianzaTest {

    private final ClasificadorConfianza clasificador = new ClasificadorConfianza();

    @Test
    void confianzaNegativaEsRechazada() { // CP-CONF-01
        assertThrows(IllegalArgumentException.class,
            () -> clasificador.clasificar(Categoria.PATOGENICA, -0.1));
    }

    @Test
    void confianzaMayorA100EsRechazada() { // CP-CONF-10
        assertThrows(IllegalArgumentException.class,
            () -> clasificador.clasificar(Categoria.PATOGENICA, 100.1));
    }

    @Test
    void confianzaNulaEsRechazada() { // CP-CONF-16
        assertThrows(IllegalArgumentException.class,
            () -> clasificador.clasificar(Categoria.PATOGENICA, null));
    }    

    @Test
    void patogenicaConConfianzaCeroRequiereRevision() { // CP-CONF-02
        assertTrue(clasificador.clasificar(Categoria.PATOGENICA, 0.0).requiereRevision());
    }

    @Test
    void patogenicaJustoDebajoDelUmbralRequiereRevision() { // CP-CONF-03
        ResultadoClasificacion r = clasificador.clasificar(Categoria.PATOGENICA, 69.9);

        assertTrue(r.requiereRevision());
        assertNull(r.categoria());
    }

    @Test
    void patogenicaEnElUmbralSeClasificaYExigeFirma() { // CP-CONF-04
        ResultadoClasificacion r = clasificador.clasificar(Categoria.PATOGENICA, 70.0);

        assertFalse(r.requiereRevision());
        assertEquals(Categoria.PATOGENICA, r.categoria());
        assertTrue(r.exigeFirma());
    }

    @Test
    void patogenicaJustoEncimaDelUmbralSeClasificaYExigeFirma() { // CP-CONF-05
        ResultadoClasificacion r = clasificador.clasificar(Categoria.PATOGENICA, 70.1);

        assertEquals(Categoria.PATOGENICA, r.categoria());
        assertTrue(r.exigeFirma());
    }

    @Test
    void patogenicaConConfianzaMaximaSeClasificaYExigeFirma() { // CP-CONF-06
        ResultadoClasificacion r = clasificador.clasificar(Categoria.PATOGENICA, 100.0);

        assertEquals(Categoria.PATOGENICA, r.categoria());
        assertTrue(r.exigeFirma());
    }


    @Test
    void benignaBajoElUmbralNoQuedaComoBenigna() { // CP-CONF-07
        ResultadoClasificacion r = clasificador.clasificar(Categoria.BENIGNA, 69.9);

        assertTrue(r.requiereRevision());
        assertNotEquals(Categoria.BENIGNA, r.categoria());
    }

    @Test
    void probablementeBenignaBajoElUmbralNoQuedaComoProbablementeBenigna() { // CP-CONF-08
        ResultadoClasificacion r = clasificador.clasificar(Categoria.PROBABLEMENTE_BENIGNA, 69.9);

        assertTrue(r.requiereRevision());
        assertNotEquals(Categoria.PROBABLEMENTE_BENIGNA, r.categoria());
    }

    @Test
    void benignaEnElUmbralSeClasificaSinFirma() { // CP-CONF-09
        ResultadoClasificacion r = clasificador.clasificar(Categoria.BENIGNA, 70.0);

        assertEquals(Categoria.BENIGNA, r.categoria());
        assertFalse(r.exigeFirma());
    }

    @Test
    void probablementeBenignaEnElUmbralSeClasificaSinFirma() { // CP-CONF-15
        ResultadoClasificacion r = clasificador.clasificar(Categoria.PROBABLEMENTE_BENIGNA, 70.0);

        assertEquals(Categoria.PROBABLEMENTE_BENIGNA, r.categoria());
        assertFalse(r.exigeFirma());
    }


    @Test
    void probablementePatogenicaJustoDebajoDelUmbralRequiereRevision() { // CP-CONF-11
        assertTrue(clasificador.clasificar(Categoria.PROBABLEMENTE_PATOGENICA, 69.9).requiereRevision());
    }

    @Test
    void probablementePatogenicaEnElUmbralSeClasificaYExigeFirma() { // CP-CONF-12
        ResultadoClasificacion r = clasificador.clasificar(Categoria.PROBABLEMENTE_PATOGENICA, 70.0);

        assertEquals(Categoria.PROBABLEMENTE_PATOGENICA, r.categoria());
        assertTrue(r.exigeFirma());
    }

    @Test
    void inciertaJustoDebajoDelUmbralRequiereRevision() { // CP-CONF-13
        assertTrue(clasificador.clasificar(Categoria.SIGNIFICADO_INCIERTO, 69.9).requiereRevision());
    }

    @Test
    void inciertaEnElUmbralSeClasificaSinFirma() { // CP-CONF-14
        ResultadoClasificacion r = clasificador.clasificar(Categoria.SIGNIFICADO_INCIERTO, 70.0);

        assertEquals(Categoria.SIGNIFICADO_INCIERTO, r.categoria());
        assertFalse(r.exigeFirma());
    }
}
