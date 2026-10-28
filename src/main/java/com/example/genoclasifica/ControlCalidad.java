package com.example.genoclasifica;

public class ControlCalidad {

    public static final double UMBRAL_Q30 = 80.0;

    public boolean esCalidadSuficiente(Double q30) {
        if (q30 == null || q30 < 0.0 || q30 > 100.0) {
            throw new IllegalArgumentException("Q30 inválido: debe estar entre 0 y 100");
        }
        return q30 >= UMBRAL_Q30;
    }
}