package com.example.genoclasifica.reclasificacion;

import com.example.genoclasifica.Categoria;
import com.example.genoclasifica.informe.Informe;
import com.example.genoclasifica.informe.InformeService;
import com.example.genoclasifica.muestra.EstadoMuestra;
import com.example.genoclasifica.muestra.Muestra;
import com.example.genoclasifica.muestra.MuestraService;
import com.example.genoclasifica.muestra.SolicitudRecepcion;
import com.example.genoclasifica.seguridad.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Reclasificación (RF-08): transición válida T12 e inválida TI8 (CP-ST-INV-05) de la sección 4.2
@SpringBootTest
@Transactional
class ReclasificacionTest {

    private static final String GEN = "MLH1";

    @Autowired
    private MuestraService muestras;

    @Autowired
    private InformeService informes;

    @Autowired
    private ReclasificacionService reclasificacion;

    private void procesada(String codigo, int posicion, Categoria categoria) throws Exception {
        String contenido = "corrida sintetica RUN-" + codigo;
        String checksum = HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(contenido.getBytes(StandardCharsets.UTF_8)));
        muestras.recibir(new SolicitudRecepcion(codigo, "RUN-" + codigo, checksum, contenido, 92.0, 1.0,
            List.of(new SolicitudRecepcion.VarianteRecibida(GEN, posicion, "SNV", categoria, 90.0))));
        if (muestras.procesar(codigo).getEstado() == EstadoMuestra.PENDIENTE_FIRMA) {
            muestras.firmar(codigo, "genetista_1", Rol.GENETISTA);
        }
    }

    private Informe emitida(String codigo, int posicion, Categoria categoria) throws Exception {
        procesada(codigo, posicion, categoria);
        return informes.obtener(codigo);
    }

    private List<Notificacion> cambiarA(int posicion, Categoria categoriaNueva) {
        return reclasificacion.reclasificar(new SolicitudReclasificacion(GEN, posicion, categoriaNueva, "ClinVar-2026-11"));
    }

    @Test
    void cambioDeCategoriaNotificaAlGenetista() throws Exception { // T12
        emitida("RCL-0001", 1001, Categoria.PATOGENICA);

        List<Notificacion> avisos = cambiarA(1001, Categoria.PROBABLEMENTE_PATOGENICA);

        assertEquals(1, avisos.size());
        Notificacion aviso = avisos.get(0);
        assertEquals("RCL-0001", aviso.getCodigoMuestra());
        assertEquals(Categoria.PATOGENICA, aviso.getCategoriaAnterior());
        assertEquals(Categoria.PROBABLEMENTE_PATOGENICA, aviso.getCategoriaNueva());
        assertEquals("GENETISTA", aviso.getDestinatario());
        assertEquals(EstadoMuestra.RECLASIFICACION_NOTIFICADA, muestras.buscar("RCL-0001").getEstado());
    }

    @Test
    void laReclasificacionNoModificaElInformeEmitido() throws Exception { // TI8 · CP-ST-INV-05
        Informe original = emitida("RCL-0002", 1002, Categoria.PATOGENICA);
        byte[] pdfOriginal = original.getContenido();

        cambiarA(1002, Categoria.BENIGNA);

        Informe despues = informes.obtener("RCL-0002");
        assertArrayEquals(pdfOriginal, despues.getContenido());
        assertEquals(original.getHashSha256(), despues.getHashSha256());
        assertEquals(original.getHashSha256(), InformeService.sha256(despues.getContenido()));
        assertEquals(Categoria.PATOGENICA, muestras.buscar("RCL-0002").getVariantes().get(0).getCategoria());
    }

    @Test
    void sinCambioDeCategoriaNoSeNotifica() throws Exception {
        emitida("RCL-0003", 1003, Categoria.PATOGENICA);

        assertTrue(cambiarA(1003, Categoria.PATOGENICA).isEmpty());
        assertEquals(EstadoMuestra.INFORME_EMITIDO, muestras.buscar("RCL-0003").getEstado());
    }

    @Test
    void repetirLaMismaActualizacionNoDuplicaAvisos() throws Exception {
        emitida("RCL-0004", 1004, Categoria.BENIGNA);

        cambiarA(1004, Categoria.SIGNIFICADO_INCIERTO);
        List<Notificacion> segunda = cambiarA(1004, Categoria.SIGNIFICADO_INCIERTO);

        assertTrue(segunda.isEmpty());
        assertEquals(1, reclasificacion.listar().stream().filter(n -> n.getCodigoMuestra().equals("RCL-0004")).count());
    }

    @Test
    void unaMuestraSinInformeEmitidoNoSeNotifica() throws Exception {
        procesada("RCL-0005", 1005, Categoria.BENIGNA);
        Muestra antes = muestras.buscar("RCL-0005");

        assertTrue(cambiarA(1005, Categoria.PATOGENICA).isEmpty());
        assertEquals(antes.getEstado(), muestras.buscar("RCL-0005").getEstado());
    }
}
