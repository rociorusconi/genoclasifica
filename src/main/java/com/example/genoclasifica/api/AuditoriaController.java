package com.example.genoclasifica.api;

import com.example.genoclasifica.auditoria.Auditoria;
import com.example.genoclasifica.auditoria.RegistroAuditoria;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Solo lectura: la API no ofrece ninguna forma de modificar ni borrar registros (RF-10)
@RestController
@RequestMapping("/api/v1/audit")
public class AuditoriaController {

    private final Auditoria auditoria;

    public AuditoriaController(Auditoria auditoria) {
        this.auditoria = auditoria;
    }

    @GetMapping
    public List<RegistroAuditoria> registros(@RequestParam(required = false) String codigo) {
        return auditoria.listar(codigo);
    }
}
