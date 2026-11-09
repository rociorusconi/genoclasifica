package com.example.genoclasifica.muestra;

public enum EstadoMuestra {
    RECIBIDA("Recibida"),
    RECHAZADA_DUPLICADA("Rechazada por duplicada"),
    RECHAZADA_INTEGRIDAD("Rechazada por integridad"),
    CALIDAD_INSUFICIENTE("Calidad insuficiente"),
    BLOQUEADA("Bloqueada"),
    CALIDAD_INSUFICIENTE_Y_BLOQUEADA("Calidad insuficiente y bloqueada"),
    CLASIFICADA("Clasificada"),
    PENDIENTE_FIRMA("Pendiente de firma"),
    FIRMADA("Firmada"),
    INFORME_EMITIDO("Informe emitido"),
    RECLASIFICACION_NOTIFICADA("Reclasificación notificada");

    private final String descripcion;

    EstadoMuestra(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
