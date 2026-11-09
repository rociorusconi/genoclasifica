package com.example.genoclasifica.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// API REST con login JWT y permisos por rol (RF-01, RF-06, RF-07, RF-09). Verifica los códigos HTTP de la sección 4.2.
@SpringBootTest
@AutoConfigureMockMvc
class ApiMuestrasTest {

    @Autowired
    private MockMvc mvc;

    private String token(String usuario) throws Exception {
        String respuesta = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"" + usuario + "\",\"clave\":\"Clave-Sintetica-123\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder pedido, String usuario) throws Exception {
        return pedido.header("Authorization", "Bearer " + token(usuario));
    }

    private String muestraJson(String codigo, String idCorrida, double q30, double contaminacion, String categoria) throws Exception {
        String contenido = "corrida sintetica " + idCorrida;
        String checksum = HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(contenido.getBytes(StandardCharsets.UTF_8)));
        return """
            {"codigo":"%s","idCorrida":"%s","checksum":"%s","contenido":"%s","q30":%s,"contaminacion":%s,
             "variantes":[{"gen":"BRCA1","posicion":43071077,"tipo":"SNV","categoriaPropuesta":"%s","confianza":90.0}]}
            """.formatted(codigo, idCorrida, checksum, contenido, q30, contaminacion, categoria);
    }

    private void recibirYProcesar(String codigo, double contaminacion, String categoria) throws Exception {
        mvc.perform(conToken(post("/api/v1/samples"), "tecnico_lab").contentType(MediaType.APPLICATION_JSON)
                .content(muestraJson(codigo, "RUN-" + codigo, 92.0, contaminacion, categoria)))
            .andExpect(status().isCreated());
        mvc.perform(conToken(post("/api/v1/samples/" + codigo + "/process"), "bio_user"))
            .andExpect(status().isOk());
    }

    @Test
    void loginConClaveIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"genetista_1\",\"clave\":\"otra\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.mensaje").value("Usuario o clave incorrectos"));
    }

    @Test
    void pedidoSinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/samples/API-0001"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAlteradoDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/samples/API-0001").header("Authorization", "Bearer " + token("auditor") + "x"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rolSinPermisoParaRecibirDevuelve403() throws Exception {
        mvc.perform(conToken(post("/api/v1/samples"), "auditor").contentType(MediaType.APPLICATION_JSON)
                .content(muestraJson("API-0002", "RUN-API-0002", 92.0, 1.0, "BENIGNA")))
            .andExpect(status().isForbidden());
    }

    @Test
    void recepcionValidaDevuelve201YQuedaRecibida() throws Exception { // T1
        mvc.perform(conToken(post("/api/v1/samples"), "tecnico_lab").contentType(MediaType.APPLICATION_JSON)
                .content(muestraJson("API-0003", "RUN-API-0003", 92.0, 1.0, "BENIGNA")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("Recibida"));
    }

    @Test
    void corridaDuplicadaDevuelve409() throws Exception { // T2
        mvc.perform(conToken(post("/api/v1/samples"), "tecnico_lab").contentType(MediaType.APPLICATION_JSON)
            .content(muestraJson("API-0004", "RUN-DUP", 92.0, 1.0, "BENIGNA")));

        mvc.perform(conToken(post("/api/v1/samples"), "tecnico_lab").contentType(MediaType.APPLICATION_JSON)
                .content(muestraJson("API-0005", "RUN-DUP", 92.0, 1.0, "BENIGNA")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.estado").value("Rechazada por duplicada"));
    }

    @Test
    void archivoCorruptoDevuelve422() throws Exception { // T3
        String corrupta = muestraJson("API-0006", "RUN-API-0006", 92.0, 1.0, "BENIGNA")
            .replace("corrida sintetica RUN-API-0006", "contenido truncado");

        mvc.perform(conToken(post("/api/v1/samples"), "tecnico_lab").contentType(MediaType.APPLICATION_JSON)
                .content(corrupta))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.estado").value("Rechazada por integridad"));
    }

    @Test
    void q30FueraDeRangoDevuelve400() throws Exception {
        mvc.perform(conToken(post("/api/v1/samples"), "tecnico_lab").contentType(MediaType.APPLICATION_JSON)
                .content(muestraJson("API-0007", "RUN-API-0007", 100.1, 1.0, "BENIGNA")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value(containsString("q30")));
    }

    @Test
    void informeSinFirmaDevuelve403() throws Exception { // TI1 · CP-ST-INV-01
        recibirYProcesar("SAMP-9921", 1.0, "PATOGENICA");

        mvc.perform(conToken(get("/api/v1/reports/SAMP-9921/pdf"), "tecnico_lab"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.mensaje").value(containsString("Requisito de firma no cumplido")));
        mvc.perform(conToken(get("/api/v1/samples/SAMP-9921"), "auditor"))
            .andExpect(jsonPath("$.estado").value("Pendiente de firma"));
    }

    @Test
    void firmaDeBioinformaticoDevuelve403() throws Exception { // TI2 · CP-ST-INV-02
        recibirYProcesar("SAMP-9922", 1.0, "PATOGENICA");

        mvc.perform(conToken(post("/api/v1/samples/SAMP-9922/sign"), "bio_user"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.mensaje").value(containsString("se requiere rol Genetista")));
        mvc.perform(conToken(get("/api/v1/samples/SAMP-9922"), "auditor"))
            .andExpect(jsonPath("$.estado").value("Pendiente de firma"));
    }

    @Test
    void informeDeMuestraBloqueadaDevuelve409() throws Exception { // TI3 · CP-ST-INV-03
        recibirYProcesar("SAMP-8810", 3.5, "PATOGENICA");

        mvc.perform(conToken(get("/api/v1/reports/SAMP-8810/pdf"), "genetista_1"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensaje").value(containsString("bloqueada")));
    }

    @Test
    void genetistaFirmaYSeEmiteElPdf() throws Exception { // T10, T11
        recibirYProcesar("API-0008", 1.0, "PATOGENICA");

        mvc.perform(conToken(post("/api/v1/samples/API-0008/sign"), "genetista_1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("Firmada"))
            .andExpect(jsonPath("$.firmadaPor").value("genetista_1"));
        mvc.perform(conToken(get("/api/v1/reports/API-0008/pdf"), "genetista_1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(content().string(containsString("%PDF-1.4")));
        mvc.perform(conToken(get("/api/v1/samples/API-0008"), "auditor"))
            .andExpect(jsonPath("$.estado").value("Informe emitido"));
    }

    @Test
    void muestraInexistenteDevuelve404() throws Exception {
        mvc.perform(conToken(get("/api/v1/samples/NO-EXISTE"), "auditor"))
            .andExpect(status().isNotFound());
    }
}
