package com.example.genoclasifica.reclasificacion;

import com.example.genoclasifica.muestra.EstadoMuestra;
import com.example.genoclasifica.muestra.Muestra;
import com.example.genoclasifica.muestra.MuestraService;
import com.example.genoclasifica.muestra.Variante;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// RF-08: detecta las variantes de informes ya emitidos cuya categoría cambiaría y avisa al genetista.
// Supuesto de la sección 3: cualquier cambio de categoría notifica. Ni la variante ni el informe se modifican.
@Service
@Transactional
public class ReclasificacionService {

    private static final Set<EstadoMuestra> CON_INFORME =
        Set.of(EstadoMuestra.INFORME_EMITIDO, EstadoMuestra.RECLASIFICACION_NOTIFICADA);

    private final MuestraService muestras;
    private final NotificacionRepository notificaciones;

    public ReclasificacionService(MuestraService muestras, NotificacionRepository notificaciones) {
        this.muestras = muestras;
        this.notificaciones = notificaciones;
    }

    public List<Notificacion> reclasificar(SolicitudReclasificacion cambio) {
        List<Notificacion> nuevas = new ArrayList<>();
        for (Muestra muestra : muestras.listar(cambio.gen(), null, null, null)) {
            if (!CON_INFORME.contains(muestra.getEstado())) {
                continue;
            }
            for (Variante v : muestra.getVariantes()) {
                if (cambiaria(v, cambio) && !yaNotificada(muestra, cambio)) {
                    nuevas.add(notificaciones.save(new Notificacion(muestra.getCodigo(), v.getGen(), v.getPosicion(),
                        v.getCategoria(), cambio.categoriaNueva(), cambio.versionBases())));
                    muestras.registrarReclasificacion(muestra.getCodigo(), v.getGen() + " " + v.getPosicion() + ": "
                        + v.getCategoria() + " -> " + cambio.categoriaNueva() + " (bases " + cambio.versionBases() + ")");
                }
            }
        }
        return nuevas;
    }

    @Transactional(readOnly = true)
    public List<Notificacion> listar() {
        return notificaciones.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    private boolean cambiaria(Variante v, SolicitudReclasificacion cambio) {
        return v.getGen().equalsIgnoreCase(cambio.gen()) && v.getPosicion().equals(cambio.posicion())
            && v.getCategoria() != null && v.getCategoria() != cambio.categoriaNueva();
    }

    // Repetir la misma actualización no duplica avisos
    private boolean yaNotificada(Muestra muestra, SolicitudReclasificacion cambio) {
        return notificaciones.existsByCodigoMuestraAndGenIgnoreCaseAndPosicionAndCategoriaNueva(
            muestra.getCodigo(), cambio.gen(), cambio.posicion(), cambio.categoriaNueva());
    }
}
