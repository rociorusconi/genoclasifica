package com.example.genoclasifica.muestra;

import com.example.genoclasifica.Categoria;
import com.example.genoclasifica.ClasificadorConfianza;
import com.example.genoclasifica.ControlCalidad;
import com.example.genoclasifica.ControlContaminacion;
import com.example.genoclasifica.auditoria.Auditoria;
import com.example.genoclasifica.seguridad.Rol;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;

import static com.example.genoclasifica.muestra.EstadoMuestra.*;

// Ciclo de vida de la muestra: transiciones T1 a T11 de la sección 4.2
@Service
@Transactional
public class MuestraService {

    private static final List<EstadoMuestra> RECHAZADAS = List.of(RECHAZADA_DUPLICADA, RECHAZADA_INTEGRIDAD);

    private final MuestraRepository muestras;
    private final Auditoria auditoria;
    private final ControlCalidad controlCalidad = new ControlCalidad();
    private final ControlContaminacion controlContaminacion = new ControlContaminacion();
    private final ClasificadorConfianza clasificador = new ClasificadorConfianza();

    public MuestraService(MuestraRepository muestras, Auditoria auditoria) {
        this.muestras = muestras;
        this.auditoria = auditoria;
    }

    public Muestra recibir(SolicitudRecepcion solicitud) {
        Muestra muestra = new Muestra(solicitud.codigo(), solicitud.idCorrida(), solicitud.checksum(),
            solicitud.q30(), solicitud.contaminacion());

        if (!esIntegra(solicitud.contenido(), solicitud.checksum())) {
            muestra.cambiarEstado(RECHAZADA_INTEGRIDAD);
        } else if (muestras.existsByIdCorridaAndChecksumAndEstadoNotIn(solicitud.idCorrida(), solicitud.checksum(), RECHAZADAS)) {
            muestra.cambiarEstado(RECHAZADA_DUPLICADA);
        } else {
            muestra.cambiarEstado(RECIBIDA);
            if (solicitud.variantes() != null) {
                solicitud.variantes().forEach(v -> muestra.agregarVariante(
                    new Variante(v.gen(), v.posicion(), v.tipo(), v.categoriaPropuesta(), v.confianza())));
            }
        }
        Muestra guardada = muestras.save(muestra);
        auditoria.registrar("RECEPCION", guardada.getCodigo(),
            "corrida=" + guardada.getIdCorrida() + "; estado=" + guardada.getEstado().getDescripcion());
        return guardada;
    }

    public Muestra procesar(String codigo) {
        Muestra muestra = buscar(codigo);
        if (muestra.getEstado() != RECIBIDA) {
            throw new TransicionInvalidaException("No se puede procesar una muestra en estado " + muestra.getEstado().getDescripcion());
        }

        boolean calidadOk = controlCalidad.esCalidadSuficiente(muestra.getQ30());
        boolean contaminacionOk = controlContaminacion.esContaminacionAceptada(muestra.getContaminacion());

        if (!calidadOk && !contaminacionOk) {
            muestra.cambiarEstado(CALIDAD_INSUFICIENTE_Y_BLOQUEADA);
        } else if (!calidadOk) {
            muestra.cambiarEstado(CALIDAD_INSUFICIENTE);
        } else if (!contaminacionOk) {
            muestra.cambiarEstado(BLOQUEADA);
        } else {
            muestra.getVariantes().forEach(v -> v.aplicar(clasificador.clasificar(v.getCategoriaPropuesta(), v.getConfianza())));
            muestra.cambiarEstado(muestra.tieneVariantesQueExigenFirma() ? PENDIENTE_FIRMA : CLASIFICADA);
        }
        auditoria.registrar("PROCESAMIENTO", codigo, "estado=" + muestra.getEstado().getDescripcion());
        return muestra;
    }

    public Muestra firmar(String codigo, String usuario, Rol rol) {
        Muestra muestra = buscar(codigo);
        if (rol != Rol.GENETISTA) {
            throw rechazo("FIRMA_RECHAZADA", codigo,
                new PermisoDenegadoException("Acceso denegado: se requiere rol Genetista para firmar diagnósticos"));
        }
        if (muestra.getEstado() != PENDIENTE_FIRMA) {
            throw rechazo("FIRMA_RECHAZADA", codigo,
                new TransicionInvalidaException("Solo se puede firmar una muestra en estado Pendiente de firma"));
        }
        muestra.registrarFirma(usuario);
        muestra.cambiarEstado(FIRMADA);
        auditoria.registrar("FIRMA", codigo, "firmada por " + usuario);
        return muestra;
    }

    public Muestra emitirInforme(String codigo) {
        Muestra muestra = buscar(codigo);
        switch (muestra.getEstado()) {
            case CLASIFICADA, FIRMADA -> muestra.cambiarEstado(INFORME_EMITIDO);
            case PENDIENTE_FIRMA -> throw rechazo("INFORME_RECHAZADO", codigo, new FirmaRequeridaException());
            case BLOQUEADA, CALIDAD_INSUFICIENTE_Y_BLOQUEADA -> throw rechazo("INFORME_RECHAZADO", codigo,
                new TransicionInvalidaException("No se puede emitir informe de una muestra bloqueada por seguridad clínica (RF-07)"));
            default -> throw rechazo("INFORME_RECHAZADO", codigo, new TransicionInvalidaException(
                "No se puede emitir informe de una muestra en estado " + muestra.getEstado().getDescripcion()));
        }
        return muestra;
    }

    // T12: solo una muestra con informe emitido pasa a "Reclasificación notificada". El informe no se toca.
    public Muestra registrarReclasificacion(String codigo, String detalle) {
        Muestra muestra = buscar(codigo);
        if (muestra.getEstado() != INFORME_EMITIDO && muestra.getEstado() != RECLASIFICACION_NOTIFICADA) {
            throw new TransicionInvalidaException(
                "Solo se notifica la reclasificación de una muestra con informe emitido; estado actual: " + muestra.getEstado().getDescripcion());
        }
        muestra.cambiarEstado(RECLASIFICACION_NOTIFICADA);
        auditoria.registrar("RECLASIFICACION_NOTIFICADA", codigo, detalle);
        return muestra;
    }

    // Los intentos rechazados también quedan auditados (postcondición de CP-ST-INV-01)
    private RuntimeException rechazo(String accion, String codigo, RuntimeException motivo) {
        auditoria.registrarRechazo(accion, codigo, motivo.getMessage());
        return motivo;
    }

    @Transactional(readOnly = true)
    public Muestra buscar(String codigo) {
        return muestras.findByCodigo(codigo).orElseThrow(() -> new MuestraNoEncontradaException(codigo));
    }

    // Filtros del portal (RF-09). Gen y categoría se buscan en la misma variante; las fechas son inclusivas.
    @Transactional(readOnly = true)
    public List<Muestra> listar(String gen, Categoria categoria, LocalDate desde, LocalDate hasta) {
        return muestras.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
            .filter(m -> desde == null || !m.getFechaRecepcion().isBefore(desde))
            .filter(m -> hasta == null || !m.getFechaRecepcion().isAfter(hasta))
            .filter(m -> (gen == null && categoria == null) || m.getVariantes().stream().anyMatch(v ->
                (gen == null || v.getGen().equalsIgnoreCase(gen)) && (categoria == null || categoria == v.getCategoria())))
            .toList();
    }

    private boolean esIntegra(String contenido, String checksumDeclarado) {
        if (contenido == null || contenido.isEmpty()) {
            return false;
        }
        return sha256(contenido).equalsIgnoreCase(checksumDeclarado);
    }

    static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
