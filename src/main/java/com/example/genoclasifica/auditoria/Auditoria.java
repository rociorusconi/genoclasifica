package com.example.genoclasifica.auditoria;

import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Registro de todo cambio (RF-10). El usuario sale del token del pedido; sin pedido HTTP queda "sistema".
@Service
public class Auditoria {

    private final AuditoriaRepository registros;

    public Auditoria(AuditoriaRepository registros) {
        this.registros = registros;
    }

    @Transactional
    public void registrar(String accion, String codigoMuestra, String detalle) {
        registros.save(new RegistroAuditoria(usuarioActual(), accion, codigoMuestra, detalle));
    }

    // Transacción propia: el intento rechazado termina en excepción y no tiene que deshacerse junto con lo demás
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarRechazo(String accion, String codigoMuestra, String motivo) {
        registros.save(new RegistroAuditoria(usuarioActual(), accion, codigoMuestra, motivo));
    }

    @Transactional(readOnly = true)
    public List<RegistroAuditoria> listar(String codigoMuestra) {
        if (codigoMuestra == null || codigoMuestra.isBlank()) {
            return registros.findAll(Sort.by("id"));
        }
        return registros.findByCodigoMuestraOrderByIdAsc(codigoMuestra);
    }

    private String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return "sistema";
        }
        return auth.getName();
    }
}
