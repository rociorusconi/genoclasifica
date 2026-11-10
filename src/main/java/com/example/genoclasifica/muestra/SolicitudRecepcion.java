package com.example.genoclasifica.muestra;

import com.example.genoclasifica.Categoria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// Datos que llegan del secuenciador; el contenido simula el archivo de la corrida
public record SolicitudRecepcion(
    @NotBlank String codigo,
    @NotBlank String idCorrida,
    @NotBlank String checksum,
    String contenido,
    @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double q30,
    @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double contaminacion,
    @Valid List<VarianteRecibida> variantes
) {

    public record VarianteRecibida(
        @NotBlank String gen,
        @NotNull Integer posicion,
        String tipo,
        @NotNull Categoria categoriaPropuesta,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double confianza
    ) {
    }
}
