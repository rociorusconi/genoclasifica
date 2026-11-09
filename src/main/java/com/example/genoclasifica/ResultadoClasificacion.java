package com.example.genoclasifica;

public record ResultadoClasificacion(Categoria categoria, boolean requiereRevision, boolean exigeFirma) {

    public static ResultadoClasificacion paraRevision() {
        return new ResultadoClasificacion(null, true, false);
    }

    public static ResultadoClasificacion clasificada(Categoria categoria) {
        return new ResultadoClasificacion(categoria, false, categoria.exigeFirma());
    }
}
