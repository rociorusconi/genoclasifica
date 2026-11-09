package com.example.genoclasifica;

public class ClasificadorConfianza {

    private static final double UMBRAL_CONFIANZA = 70.0;

    public ResultadoClasificacion clasificar(Categoria categoria, Double confianza) {
        if (confianza == null || confianza < 0.0 || confianza > 100.0) {
            throw new IllegalArgumentException("La confianza debe estar entre 0 y 100");
        }

        // Bajo 70 % no se asigna categoría: la variante queda para revisión
        if (confianza < UMBRAL_CONFIANZA) {
            return ResultadoClasificacion.paraRevision();
        }
        return ResultadoClasificacion.clasificada(categoria);
    }
}
