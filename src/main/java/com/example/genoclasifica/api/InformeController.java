package com.example.genoclasifica.api;

import com.example.genoclasifica.muestra.EstadoMuestra;
import com.example.genoclasifica.muestra.Muestra;
import com.example.genoclasifica.muestra.MuestraService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class InformeController {

    private final MuestraService service;
    private final GeneradorPdf generadorPdf;

    public InformeController(MuestraService service, GeneradorPdf generadorPdf) {
        this.service = service;
        this.generadorPdf = generadorPdf;
    }

    // La primera vez emite el informe; si ya estaba emitido, lo vuelve a entregar
    @GetMapping("/{codigo}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String codigo) {
        Muestra muestra = service.buscar(codigo);
        if (muestra.getEstado() != EstadoMuestra.INFORME_EMITIDO) {
            muestra = service.emitirInforme(codigo);
        }
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=informe-" + codigo + ".pdf")
            .body(generadorPdf.generar(muestra));
    }
}
