package com.example.genoclasifica.api;

import com.example.genoclasifica.Categoria;
import com.example.genoclasifica.muestra.Muestra;
import com.example.genoclasifica.muestra.MuestraService;
import com.example.genoclasifica.muestra.SolicitudRecepcion;
import com.example.genoclasifica.seguridad.Rol;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/samples")
public class MuestraController {

    private final MuestraService service;

    public MuestraController(MuestraService service) {
        this.service = service;
    }

    // 201 si se recibe, 409 si la corrida ya se procesó, 422 si el archivo no es íntegro (RF-01)
    @PostMapping
    public ResponseEntity<MuestraResponse> recibir(@Valid @RequestBody SolicitudRecepcion solicitud) {
        Muestra muestra = service.recibir(solicitud);
        HttpStatus status = switch (muestra.getEstado()) {
            case RECHAZADA_DUPLICADA -> HttpStatus.CONFLICT;
            case RECHAZADA_INTEGRIDAD -> HttpStatus.UNPROCESSABLE_CONTENT;
            default -> HttpStatus.CREATED;
        };
        return ResponseEntity.status(status).body(MuestraResponse.de(muestra));
    }

    @PostMapping("/{codigo}/process")
    public MuestraResponse procesar(@PathVariable String codigo) {
        return MuestraResponse.de(service.procesar(codigo));
    }

    // Quién firma y con qué rol sale del token, no de lo que mande el cliente
    @PostMapping("/{codigo}/sign")
    public MuestraResponse firmar(@PathVariable String codigo, @AuthenticationPrincipal Jwt token) {
        Rol rol = Rol.valueOf(token.getClaimAsString("rol"));
        return MuestraResponse.de(service.firmar(codigo, token.getSubject(), rol));
    }

    // Sin parámetros devuelve todas. La categoría es la final, la que quedó después de procesar.
    @GetMapping
    public List<MuestraResponse> listar(@RequestParam(required = false) String gen,
                                        @RequestParam(required = false) Categoria categoria,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        String genBuscado = gen == null || gen.isBlank() ? null : gen.trim();
        return service.listar(genBuscado, categoria, desde, hasta).stream().map(MuestraResponse::de).toList();
    }

    @GetMapping("/{codigo}")
    public MuestraResponse consultar(@PathVariable String codigo) {
        return MuestraResponse.de(service.buscar(codigo));
    }
}
