package com.example.genoclasifica.muestra;

import com.example.genoclasifica.Categoria;
import com.example.genoclasifica.seguridad.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.genoclasifica.muestra.EstadoMuestra.*;
import static org.junit.jupiter.api.Assertions.*;

// Transición de estados de la muestra (sección 4.2): válidas T1 a T11 e inválidas TI1 a TI11.
@SpringBootTest
@Transactional
class MuestraServiceTest {

    private static final String CONTENIDO = "corrida sintetica";

    @Autowired
    private MuestraService service;

    private SolicitudRecepcion solicitud(String codigo, String idCorrida, double q30, double contaminacion,
                                         Categoria categoria, double confianza) {
        String contenido = CONTENIDO + " " + idCorrida;
        return new SolicitudRecepcion(codigo, idCorrida, MuestraService.sha256(contenido), contenido, q30, contaminacion,
            List.of(new SolicitudRecepcion.VarianteRecibida("BRCA1", 43071077, "SNV", categoria, confianza)));
    }

    private Muestra recibida(String codigo, double q30, double contaminacion, Categoria categoria) {
        return service.recibir(solicitud(codigo, "RUN-" + codigo, q30, contaminacion, categoria, 90.0));
    }

    private Muestra pendienteDeFirma(String codigo) {
        recibida(codigo, 92.0, 1.0, Categoria.PATOGENICA);
        return service.procesar(codigo);
    }

    @Test
    void corridaIntegraYNuevaQuedaRecibida() { // T1
        assertEquals(RECIBIDA, recibida("SAMP-0001", 92.0, 1.0, Categoria.BENIGNA).getEstado());
    }

    @Test
    void corridaYaProcesadaSeRechazaPorDuplicada() { // T2
        service.recibir(solicitud("SAMP-0002", "RUN-77", 92.0, 1.0, Categoria.BENIGNA, 90.0));

        Muestra repetida = service.recibir(solicitud("SAMP-0003", "RUN-77", 92.0, 1.0, Categoria.BENIGNA, 90.0));

        assertEquals(RECHAZADA_DUPLICADA, repetida.getEstado());
        assertTrue(repetida.getVariantes().isEmpty());
    }

    @Test
    void archivoCorruptoSeRechazaPorIntegridad() { // T3
        SolicitudRecepcion corrupta = new SolicitudRecepcion("SAMP-0004", "RUN-0004", "checksum-que-no-coincide",
            "contenido truncado", 92.0, 1.0, List.of());

        assertEquals(RECHAZADA_INTEGRIDAD, service.recibir(corrupta).getEstado());
    }

    @Test
    void q30BajoDejaLaMuestraConCalidadInsuficiente() { // T4
        recibida("SAMP-0005", 75.0, 1.0, Categoria.PATOGENICA);

        Muestra muestra = service.procesar("SAMP-0005");

        assertEquals(CALIDAD_INSUFICIENTE, muestra.getEstado());
        assertNull(muestra.getVariantes().get(0).getCategoria());
    }

    @Test
    void contaminacionAltaBloqueaLaMuestra() { // T5
        recibida("SAMP-0006", 92.0, 3.5, Categoria.PATOGENICA);

        assertEquals(BLOQUEADA, service.procesar("SAMP-0006").getEstado());
    }

    @Test
    void q30BajoYContaminacionAltaDejaAmbosEstados() { // T6
        recibida("SAMP-0007", 75.0, 3.5, Categoria.PATOGENICA);

        assertEquals(CALIDAD_INSUFICIENTE_Y_BLOQUEADA, service.procesar("SAMP-0007").getEstado());
    }

    @Test
    void muestraAceptadaSinPatogenicasQuedaClasificada() { // T7
        recibida("SAMP-0008", 92.0, 1.0, Categoria.BENIGNA);

        Muestra muestra = service.procesar("SAMP-0008");

        assertEquals(CLASIFICADA, muestra.getEstado());
        assertEquals(Categoria.BENIGNA, muestra.getVariantes().get(0).getCategoria());
    }

    @Test
    void muestraConPatogenicaQuedaPendienteDeFirma() { // T7 + T8
        assertEquals(PENDIENTE_FIRMA, pendienteDeFirma("SAMP-0009").getEstado());
    }

    @Test
    void muestraSinPatogenicasEmiteInformeSinFirma() { // T9
        recibida("SAMP-0010", 92.0, 1.0, Categoria.BENIGNA);
        service.procesar("SAMP-0010");

        assertEquals(INFORME_EMITIDO, service.emitirInforme("SAMP-0010").getEstado());
    }

    @Test
    void genetistaFirmaLaMuestra() { // T10
        pendienteDeFirma("SAMP-0011");

        Muestra muestra = service.firmar("SAMP-0011", "genetista_1", Rol.GENETISTA);

        assertEquals(FIRMADA, muestra.getEstado());
        assertEquals("genetista_1", muestra.getFirmadaPor());
    }

    @Test
    void muestraFirmadaEmiteInforme() { // T11
        pendienteDeFirma("SAMP-0012");
        service.firmar("SAMP-0012", "genetista_1", Rol.GENETISTA);

        assertEquals(INFORME_EMITIDO, service.emitirInforme("SAMP-0012").getEstado());
    }

    @Test
    void informeSinFirmaSeRechaza() { // TI1 · CP-ST-INV-01
        pendienteDeFirma("SAMP-9921");

        assertThrows(FirmaRequeridaException.class, () -> service.emitirInforme("SAMP-9921"));
        assertEquals(PENDIENTE_FIRMA, service.buscar("SAMP-9921").getEstado());
    }

    @Test
    void firmaDeUnRolNoGenetistaSeRechaza() { // TI2 · CP-ST-INV-02
        pendienteDeFirma("SAMP-9922");

        assertThrows(PermisoDenegadoException.class, () -> service.firmar("SAMP-9922", "bio_user", Rol.BIOINFORMATICO));
        assertThrows(PermisoDenegadoException.class, () -> service.firmar("SAMP-9922", "tecnico_lab", Rol.TECNICO));
        assertEquals(PENDIENTE_FIRMA, service.buscar("SAMP-9922").getEstado());
    }

    @Test
    void informeDeMuestraBloqueadaSeRechaza() { // TI3 · CP-ST-INV-03
        recibida("SAMP-8810", 92.0, 3.5, Categoria.PATOGENICA);
        service.procesar("SAMP-8810");

        assertThrows(TransicionInvalidaException.class, () -> service.emitirInforme("SAMP-8810"));
        assertEquals(BLOQUEADA, service.buscar("SAMP-8810").getEstado());
    }

    @Test
    void muestraBloqueadaNoSeVuelveAClasificar() { // TI4 · CP-ST-INV-04
        recibida("SAMP-0013", 92.0, 3.5, Categoria.PATOGENICA);
        service.procesar("SAMP-0013");

        assertThrows(TransicionInvalidaException.class, () -> service.procesar("SAMP-0013"));
        assertEquals(BLOQUEADA, service.buscar("SAMP-0013").getEstado());
    }

    @Test
    void muestraConCalidadInsuficienteNoSeVuelveAClasificar() { // TI5 · CP-ST-INV-04
        recibida("SAMP-5011", 75.0, 1.0, Categoria.PATOGENICA);
        service.procesar("SAMP-5011");

        assertThrows(TransicionInvalidaException.class, () -> service.procesar("SAMP-5011"));
        Muestra muestra = service.buscar("SAMP-5011");
        assertEquals(CALIDAD_INSUFICIENTE, muestra.getEstado());
        assertNull(muestra.getVariantes().get(0).getCategoria());
    }

    @Test
    void informeDeMuestraConCalidadInsuficienteSeRechaza() { // TI6
        recibida("SAMP-0014", 75.0, 1.0, Categoria.BENIGNA);
        service.procesar("SAMP-0014");

        assertThrows(TransicionInvalidaException.class, () -> service.emitirInforme("SAMP-0014"));
        assertEquals(CALIDAD_INSUFICIENTE, service.buscar("SAMP-0014").getEstado());
    }

    @Test
    void informeDeMuestraConCalidadInsuficienteYBloqueadaSeRechaza() { // TI7
        recibida("SAMP-0015", 75.0, 3.5, Categoria.BENIGNA);
        service.procesar("SAMP-0015");

        assertThrows(TransicionInvalidaException.class, () -> service.emitirInforme("SAMP-0015"));
        assertEquals(CALIDAD_INSUFICIENTE_Y_BLOQUEADA, service.buscar("SAMP-0015").getEstado());
    }

    @Test
    void firmaSinHaberClasificadoSeRechaza() { // TI9
        recibida("SAMP-0016", 92.0, 1.0, Categoria.PATOGENICA);

        assertThrows(TransicionInvalidaException.class, () -> service.firmar("SAMP-0016", "genetista_1", Rol.GENETISTA));
        assertEquals(RECIBIDA, service.buscar("SAMP-0016").getEstado());
    }

    @Test
    void corridaDuplicadaNoSeProcesa() { // TI10
        service.recibir(solicitud("SAMP-0017", "RUN-88", 92.0, 1.0, Categoria.BENIGNA, 90.0));
        service.recibir(solicitud("SAMP-0018", "RUN-88", 92.0, 1.0, Categoria.BENIGNA, 90.0));

        assertThrows(TransicionInvalidaException.class, () -> service.procesar("SAMP-0018"));
        assertEquals(RECHAZADA_DUPLICADA, service.buscar("SAMP-0018").getEstado());
    }

    @Test
    void corridaCorruptaNoSeProcesa() { // TI11
        service.recibir(new SolicitudRecepcion("SAMP-0019", "RUN-0019", "checksum-que-no-coincide",
            "contenido truncado", 92.0, 1.0, List.of()));

        assertThrows(TransicionInvalidaException.class, () -> service.procesar("SAMP-0019"));
        assertEquals(RECHAZADA_INTEGRIDAD, service.buscar("SAMP-0019").getEstado());
    }
}
