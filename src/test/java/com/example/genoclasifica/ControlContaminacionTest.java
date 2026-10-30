package com.example.genoclasifica;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ControlContaminacionTest {

    private final ControlContaminacion control = new ControlContaminacion();

    @Test
    void contaminacionMenorACeroEsRechazada() { // CP-CON-01
        assertThrows(IllegalArgumentException.class, 
            () -> control.esContaminacionAceptada(-0.1));
    }

    @Test
    void contaminacionMinimaValidaNoBloquea() { // CP-CON-02
        assertTrue(control.esContaminacionAceptada(0.0));
    }

    @Test
    void contaminacionJustoDebajoDelUmbralNoBloquea() { // CP-CON-03
        assertTrue(control.esContaminacionAceptada(2.9));
    }

    @Test
    void contaminacionLimiteExactoNoBloquea() { // CP-CON-04
        assertTrue(control.esContaminacionAceptada(3.0));
    }

    @Test
    void contaminacionJustoEncimaDelUmbralBloquea() { // CP-CON-05
        assertFalse(control.esContaminacionAceptada(3.1));
    }

    @Test
    void contaminacionValidaMaximaBloquea() { // CP-CON-06
        assertFalse(control.esContaminacionAceptada(100.0));
    }

    @Test
    void contaminacionMayorA100EsRechazada() { // CP-CON-07
        assertThrows(IllegalArgumentException.class, 
            () -> control.esContaminacionAceptada(100.1));
    }
}