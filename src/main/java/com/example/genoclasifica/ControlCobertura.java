package com.example.genoclasifica;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ControlCobertura {

    private static final int UMBRAL_COBERTURA = 30;

    public boolean esEvaluable(Integer cobertura) {
        if (cobertura == null || cobertura < 0) {
            throw new IllegalArgumentException("La cobertura no puede ser negativa");
        }

        // Con menos de 30 lecturas la posición queda "no evaluable"
        return cobertura >= UMBRAL_COBERTURA;
    }

    public List<Integer> posicionesNoEvaluables(Map<Integer, Integer> coberturaPorPosicion) {
        return coberturaPorPosicion.entrySet().stream()
            .filter(entrada -> !esEvaluable(entrada.getValue()))
            .map(Map.Entry::getKey)
            .sorted()
            .toList();
    }

    public String resumenNoEvaluables(Map<Integer, Integer> coberturaPorPosicion) {
        List<Integer> posiciones = posicionesNoEvaluables(coberturaPorPosicion);
        if (posiciones.isEmpty()) {
            return "ninguna";
        }
        return posiciones.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(", "));
    }
}
