package com.example.genoclasifica;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ControlCalidadTest {

    private final ControlCalidad control = new ControlCalidad();

    @Test
    void q30DebajoDelUmbralEsInsuficiente() { // CP-Q30-03
        assertFalse(control.esCalidadSuficiente(50.0));
    }    

    @Test
    void q30JustoDebajoDelUmbralEsInsuficiente() { // CP-Q30-04
        assertFalse(control.esCalidadSuficiente(79.9));
    }

    @Test
    void q30ExactoEnElUmbralEsAceptado() { // CP-Q30-05
        assertTrue(control.esCalidadSuficiente(80.0));
    }

    @Test
    void q30JustoEncimaDelUmbralEsAceptado() { // CP-Q30-06
        assertTrue(control.esCalidadSuficiente(80.1));
    }
    
    @Test
    void q30EnElLimiteDelUmbralEsAceptado() { // CP-Q30-07
        assertTrue(control.esCalidadSuficiente(100.0));
    }
    
    @Test
    void q30NuloEsRechazado() { // CP-Q30-01
        assertThrows(IllegalArgumentException.class,
            () -> control.esCalidadSuficiente(null));
    }

    @Test
    void q30NegativoEsRechazado() { // CP-Q30-02
        assertThrows(IllegalArgumentException.class,
            () -> control.esCalidadSuficiente(-0.1));
    }

    @Test
    void q30MayorA100EsRechazado() { // CP-Q30-08
        assertThrows(IllegalArgumentException.class,
            () -> control.esCalidadSuficiente(100.1));    
    }
}