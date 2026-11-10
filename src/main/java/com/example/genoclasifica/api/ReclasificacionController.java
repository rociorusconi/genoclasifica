package com.example.genoclasifica.api;

import com.example.genoclasifica.reclasificacion.Notificacion;
import com.example.genoclasifica.reclasificacion.ReclasificacionService;
import com.example.genoclasifica.reclasificacion.SolicitudReclasificacion;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ReclasificacionController {

    private final ReclasificacionService service;

    public ReclasificacionController(ReclasificacionService service) {
        this.service = service;
    }

    // Devuelve los avisos generados; lista vacía si ningún informe emitido cambiaría
    @PostMapping("/reclassifications")
    public List<Notificacion> reclasificar(@Valid @RequestBody SolicitudReclasificacion cambio) {
        return service.reclasificar(cambio);
    }

    @GetMapping("/notifications")
    public List<Notificacion> notificaciones() {
        return service.listar();
    }
}
