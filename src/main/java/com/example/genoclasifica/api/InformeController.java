package com.example.genoclasifica.api;

import com.example.genoclasifica.informe.InformeService;
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

    private final InformeService informes;

    public InformeController(InformeService informes) {
        this.informes = informes;
    }

    // La primera vez emite y guarda el informe; después entrega siempre el mismo archivo
    @GetMapping("/{codigo}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String codigo) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=informe-" + codigo + ".pdf")
            .body(informes.obtener(codigo).getContenido());
    }
}
