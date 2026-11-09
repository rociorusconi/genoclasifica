package com.example.genoclasifica;

public class ClasificadorConfianza {

    public ResultadoClasificacion clasificar(Categoria categoria, Double confianza) {
        if (confianza == null || confianza < 0.0 || confianza > 100.0) {
            throw new IllegalArgumentException("La confianza debe estar entre 0 y 100");
        }

        // Bajo 70 % no se asigna categoría: la variante queda para revisión
        if (confianza < 70.0) {
            return new ResultadoClasificacion(null, true, false);
        }

        boolean exigeFirma = categoria == Categoria.PATOGENICA
            || categoria == Categoria.PROBABLEMENTE_PATOGENICA;
        return new ResultadoClasificacion(categoria, false, exigeFirma);
    }
}
