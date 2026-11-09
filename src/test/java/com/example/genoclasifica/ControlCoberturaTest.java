package com.example.genoclasifica;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Cobertura por posición (RF-02 y RF-07): menos de 30 lecturas es "no evaluable".
class ControlCoberturaTest {

    private final ControlCobertura control = new ControlCobertura();

    @Test
    void coberturaNegativaEsRechazada() { // CP-COB-01
        assertThrows(IllegalArgumentException.class,
            () -> control.esEvaluable(-1));
    }

    @Test
    void coberturaCeroNoEsEvaluable() { // CP-COB-02
        assertFalse(control.esEvaluable(0));
    }

    @Test
    void coberturaJustoDebajoDelUmbralNoEsEvaluable() { // CP-COB-03
        assertFalse(control.esEvaluable(29));
    }

    @Test
    void coberturaExactaEnElUmbralEsEvaluable() { // CP-COB-04
        assertTrue(control.esEvaluable(30));
    }

    @Test
    void coberturaJustoEncimaDelUmbralEsEvaluable() { // CP-COB-05
        assertTrue(control.esEvaluable(31));
    }

    @Test
    void sinPosicionesBajoElUmbralElInformeDiceNinguna() { // CP-COB-06
        Map<Integer, Integer> cobertura = Map.of(1001, 30, 1002, 45, 1003, 120);

        assertTrue(control.posicionesNoEvaluables(cobertura).isEmpty());
        assertEquals("ninguna", control.resumenNoEvaluables(cobertura));
    }

    @Test
    void soloLasPosicionesBajoElUmbralSeListan() { // CP-COB-07
        Map<Integer, Integer> cobertura = Map.of(2001, 29, 2002, 30, 2003, 31);

        assertEquals(List.of(2001), control.posicionesNoEvaluables(cobertura));
        assertEquals("2001", control.resumenNoEvaluables(cobertura));
    }

    @Test
    void coberturaNulaEsRechazadaConMensajeDeObligatoria() { // CP-COB-08
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> control.esEvaluable(null));

        assertEquals("La cobertura es obligatoria", error.getMessage());
    }
}
