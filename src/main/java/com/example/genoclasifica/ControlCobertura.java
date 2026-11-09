package com.example.genoclasifica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ControlCobertura {

    public boolean esEvaluable(Integer cobertura) {
        if (cobertura == null || cobertura < 0) {
            throw new IllegalArgumentException("La cobertura no puede ser negativa");
        }

        // Con menos de 30 lecturas la posición queda "no evaluable"
        return cobertura >= 30;
    }

    public List<Integer> posicionesNoEvaluables(Map<Integer, Integer> coberturaPorPosicion) {
        List<Integer> posiciones = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entrada : coberturaPorPosicion.entrySet()) {
            if (!esEvaluable(entrada.getValue())) {
                posiciones.add(entrada.getKey());
            }
        }
        Collections.sort(posiciones);
        return posiciones;
    }

    public String resumenNoEvaluables(Map<Integer, Integer> coberturaPorPosicion) {
        List<Integer> posiciones = posicionesNoEvaluables(coberturaPorPosicion);
        if (posiciones.isEmpty()) {
            return "ninguna";
        }

        List<String> textos = new ArrayList<>();
        for (Integer posicion : posiciones) {
            textos.add(String.valueOf(posicion));
        }
        return String.join(", ", textos);
    }
}
