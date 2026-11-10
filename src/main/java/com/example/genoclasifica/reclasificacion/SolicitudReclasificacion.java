package com.example.genoclasifica.reclasificacion;

import com.example.genoclasifica.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Simula una actualización de las bases de anotación que cambia la categoría de una variante
public record SolicitudReclasificacion(@NotBlank String gen, @NotNull Integer posicion,
                                       @NotNull Categoria categoriaNueva, @NotBlank String versionBases) {
}
