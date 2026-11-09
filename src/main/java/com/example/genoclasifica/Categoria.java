package com.example.genoclasifica;

public enum Categoria {
    BENIGNA(false),
    PROBABLEMENTE_BENIGNA(false),
    SIGNIFICADO_INCIERTO(false),
    PROBABLEMENTE_PATOGENICA(true),
    PATOGENICA(true);

    private final boolean exigeFirma;

    Categoria(boolean exigeFirma) {
        this.exigeFirma = exigeFirma;
    }

    public boolean exigeFirma() {
        return exigeFirma;
    }
}
