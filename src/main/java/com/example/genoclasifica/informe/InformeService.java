package com.example.genoclasifica.informe;

import com.example.genoclasifica.auditoria.Auditoria;
import com.example.genoclasifica.muestra.Muestra;
import com.example.genoclasifica.muestra.MuestraService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@Transactional
public class InformeService {

    private final MuestraService muestras;
    private final InformeRepository informes;
    private final GeneradorPdf generadorPdf;
    private final Auditoria auditoria;
    private final String versionGenoma;
    private final String versionBases;
    private final String versionModelo;

    public InformeService(MuestraService muestras, InformeRepository informes, GeneradorPdf generadorPdf, Auditoria auditoria,
                          @Value("${genoclasifica.versiones.genoma}") String versionGenoma,
                          @Value("${genoclasifica.versiones.bases}") String versionBases,
                          @Value("${genoclasifica.versiones.modelo}") String versionModelo) {
        this.muestras = muestras;
        this.informes = informes;
        this.generadorPdf = generadorPdf;
        this.auditoria = auditoria;
        this.versionGenoma = versionGenoma;
        this.versionBases = versionBases;
        this.versionModelo = versionModelo;
    }

    // Si ya se emitió, devuelve el mismo archivo guardado: un informe emitido no se vuelve a generar (RF-08)
    public Informe obtener(String codigo) {
        return informes.findByCodigoMuestra(codigo).orElseGet(() -> emitir(codigo));
    }

    private Informe emitir(String codigo) {
        Muestra muestra = muestras.emitirInforme(codigo);
        byte[] pdf = generadorPdf.generar(muestra);
        Informe informe = informes.save(new Informe(codigo, muestra.getIdCorrida(), pdf, sha256(pdf),
            versionGenoma, versionBases, versionModelo));
        auditoria.registrar("INFORME_EMITIDO", codigo, "corrida=" + informe.getIdCorrida()
            + "; genoma=" + versionGenoma + "; bases=" + versionBases + "; modelo=" + versionModelo
            + "; sha256=" + informe.getHashSha256());
        return informe;
    }

    public static String sha256(byte[] datos) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(datos));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
