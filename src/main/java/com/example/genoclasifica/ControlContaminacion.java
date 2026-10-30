package com.example.genoclasifica;

public class ControlContaminacion {

    public boolean esContaminacionAceptada(Double contaminacion) {
        if (contaminacion == null || contaminacion < 0.0 || contaminacion > 100.0) {
            throw new IllegalArgumentException("El porcentaje de contaminación debe estar entre 0 y 100");
        }
        
        // Umbral crítico: <= 3.0 % NO se bloquea (retorna true)
        // > 3.0 % la muestra queda BLOQUEADA (retorna false)
        return contaminacion <= 3.0;
    }
}