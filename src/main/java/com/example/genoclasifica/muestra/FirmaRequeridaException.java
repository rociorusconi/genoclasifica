package com.example.genoclasifica.muestra;

public class FirmaRequeridaException extends TransicionInvalidaException {

    public FirmaRequeridaException() {
        super("Requisito de firma no cumplido: la muestra contiene variantes patogénicas que requieren firma del genetista");
    }
}
