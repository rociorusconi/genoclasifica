package com.example.genoclasifica.muestra;

public class MuestraNoEncontradaException extends RuntimeException {

    public MuestraNoEncontradaException(String codigo) {
        super("No existe la muestra " + codigo);
    }
}
