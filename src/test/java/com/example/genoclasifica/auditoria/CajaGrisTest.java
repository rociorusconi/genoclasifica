package com.example.genoclasifica.auditoria;

import com.example.genoclasifica.Categoria;
import com.example.genoclasifica.informe.InformeService;
import com.example.genoclasifica.muestra.*;
import com.example.genoclasifica.reclasificacion.ReclasificacionService;
import com.example.genoclasifica.reclasificacion.SolicitudReclasificacion;
import com.example.genoclasifica.seguridad.Rol;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Caja gris (sección 4.3): se usa el sistema por su servicio y se verifica el resultado consultando la base con SQL
@SpringBootTest
@Transactional
class CajaGrisTest {

    @Autowired
    private MuestraService muestras;

    @Autowired
    private InformeService informes;

    @Autowired
    private ReclasificacionService reclasificacion;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager em;

    private void procesada(String codigo, double contaminacion, Categoria categoria) throws Exception {
        String contenido = "corrida sintetica RUN-" + codigo;
        String checksum = HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(contenido.getBytes(StandardCharsets.UTF_8)));
        muestras.recibir(new SolicitudRecepcion(codigo, "RUN-" + codigo, checksum, contenido, 92.0, contaminacion,
            List.of(new SolicitudRecepcion.VarianteRecibida("PMS2", 2002, "SNV", categoria, 90.0))));
        muestras.procesar(codigo);
    }

    private void emitida(String codigo) throws Exception {
        procesada(codigo, 1.0, Categoria.PATOGENICA);
        muestras.firmar(codigo, "genetista_1", Rol.GENETISTA);
        informes.obtener(codigo);
        em.flush();
    }

    // Falla por el trigger y no por otro motivo (por ejemplo, una columna mal escrita)
    private void rechazadaPorInalterable(String sql, String codigo) {
        DataAccessException error = assertThrows(DataAccessException.class, () -> jdbc.update(sql, codigo));
        assertTrue(error.getMessage().contains("es inalterable"), error.getMessage());
    }

    private int contar(String sql, Object... parametros) {
        return jdbc.queryForObject(sql, Integer.class, parametros);
    }

    @Test
    void muestraBloqueadaNoGuardaInforme() throws Exception { // CP-CG-01
        procesada("CG-0001", 3.5, Categoria.PATOGENICA);

        assertThrows(TransicionInvalidaException.class, () -> informes.obtener("CG-0001"));
        em.flush();

        assertEquals("BLOQUEADA", jdbc.queryForObject("SELECT ESTADO FROM MUESTRA WHERE CODIGO = ?", String.class, "CG-0001"));
        assertEquals(0, contar("SELECT COUNT(*) FROM INFORME WHERE CODIGO_MUESTRA = ?", "CG-0001"));
    }

    @Test
    void laAuditoriaVinculaMuestraCorridaYVersiones() throws Exception { // CP-CG-02
        emitida("CG-0002");

        String detalle = jdbc.queryForObject(
            "SELECT DETALLE FROM AUDITORIA WHERE CODIGO_MUESTRA = ? AND ACCION = 'INFORME_EMITIDO'", String.class, "CG-0002");
        assertTrue(detalle.contains("corrida=RUN-CG-0002"));
        assertTrue(detalle.contains("genoma=GRCh38"));
        assertTrue(detalle.contains("bases=ClinVar-2026-10"));
        assertTrue(detalle.contains("modelo=4.2.0"));
        assertEquals(1, contar("SELECT COUNT(*) FROM INFORME WHERE CODIGO_MUESTRA = ? AND VERSION_GENOMA = 'GRCh38'"
            + " AND VERSION_BASES = 'ClinVar-2026-10' AND VERSION_MODELO = '4.2.0'", "CG-0002"));
    }

    @Test
    void laReclasificacionNoCambiaElHashYRegistraLaNotificacion() throws Exception { // CP-CG-03
        emitida("CG-0003");
        String hashAntes = jdbc.queryForObject("SELECT HASH_SHA256 FROM INFORME WHERE CODIGO_MUESTRA = ?", String.class, "CG-0003");

        reclasificacion.reclasificar(new SolicitudReclasificacion("PMS2", 2002, Categoria.SIGNIFICADO_INCIERTO, "ClinVar-2026-11"));
        em.flush();

        String hashDespues = jdbc.queryForObject("SELECT HASH_SHA256 FROM INFORME WHERE CODIGO_MUESTRA = ?", String.class, "CG-0003");
        byte[] pdfGuardado = jdbc.queryForObject("SELECT CONTENIDO FROM INFORME WHERE CODIGO_MUESTRA = ?", byte[].class, "CG-0003");
        assertEquals(hashAntes, hashDespues);
        assertEquals(hashAntes, InformeService.sha256(pdfGuardado));
        assertEquals(1, contar("SELECT COUNT(*) FROM NOTIFICACION WHERE CODIGO_MUESTRA = ?", "CG-0003"));
    }

    @Test
    void laAuditoriaNoAdmiteUpdateNiDelete() throws Exception { // CP-CG-04
        emitida("CG-0004");
        int registros = contar("SELECT COUNT(*) FROM AUDITORIA WHERE CODIGO_MUESTRA = ?", "CG-0004");

        rechazadaPorInalterable("UPDATE AUDITORIA SET DETALLE = 'alterado' WHERE CODIGO_MUESTRA = ?", "CG-0004");
        rechazadaPorInalterable("DELETE FROM AUDITORIA WHERE CODIGO_MUESTRA = ?", "CG-0004");

        assertTrue(registros > 0);
        assertEquals(registros, contar("SELECT COUNT(*) FROM AUDITORIA WHERE CODIGO_MUESTRA = ?", "CG-0004"));
        assertEquals(0, contar("SELECT COUNT(*) FROM AUDITORIA WHERE DETALLE = 'alterado'"));
    }

    @Test
    void elInformeGuardadoNoAdmiteUpdate() throws Exception { // TI8 a nivel de base de datos
        emitida("CG-0005");

        rechazadaPorInalterable("UPDATE INFORME SET HASH_SHA256 = 'otro' WHERE CODIGO_MUESTRA = ?", "CG-0005");
    }

    @Test
    void elIntentoDeInformeSinFirmaQuedaAuditado() throws Exception { // postcondición de CP-ST-INV-01
        procesada("CG-0006", 1.0, Categoria.PATOGENICA);

        assertThrows(FirmaRequeridaException.class, () -> informes.obtener("CG-0006"));

        assertEquals(1, contar("SELECT COUNT(*) FROM AUDITORIA WHERE CODIGO_MUESTRA = ? AND ACCION = 'INFORME_RECHAZADO'", "CG-0006"));
    }

    @Test
    void laFirmaDeUnRolNoAutorizadoQuedaAuditada() throws Exception { // CP-ST-INV-02
        procesada("CG-0007", 1.0, Categoria.PATOGENICA);

        assertThrows(PermisoDenegadoException.class, () -> muestras.firmar("CG-0007", "bio_user", Rol.BIOINFORMATICO));

        assertEquals(1, contar("SELECT COUNT(*) FROM AUDITORIA WHERE CODIGO_MUESTRA = ? AND ACCION = 'FIRMA_RECHAZADA'", "CG-0007"));
    }
}
