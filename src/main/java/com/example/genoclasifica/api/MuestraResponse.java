package com.example.genoclasifica.api;

import com.example.genoclasifica.Categoria;
import com.example.genoclasifica.muestra.Muestra;
import com.example.genoclasifica.muestra.Variante;

import java.time.LocalDate;
import java.util.List;

// Lo que devuelve la API: se arma a partir de la entidad para no exponerla directamente
public record MuestraResponse(String codigo, String idCorrida, String estado, Double q30, Double contaminacion,
                              LocalDate fechaRecepcion, String firmadaPor, List<VarianteResponse> variantes) {

    public record VarianteResponse(String gen, Integer posicion, String tipo, Categoria categoriaPropuesta,
                                   Double confianza, Categoria categoria, boolean requiereRevision, boolean exigeFirma) {

        static VarianteResponse de(Variante v) {
            return new VarianteResponse(v.getGen(), v.getPosicion(), v.getTipo(), v.getCategoriaPropuesta(),
                v.getConfianza(), v.getCategoria(), v.isRequiereRevision(), v.isExigeFirma());
        }
    }

    public static MuestraResponse de(Muestra m) {
        return new MuestraResponse(m.getCodigo(), m.getIdCorrida(), m.getEstado().getDescripcion(), m.getQ30(),
            m.getContaminacion(), m.getFechaRecepcion(), m.getFirmadaPor(),
            m.getVariantes().stream().map(VarianteResponse::de).toList());
    }
}
