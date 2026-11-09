package com.example.genoclasifica.api;

import com.example.genoclasifica.muestra.Muestra;
import com.example.genoclasifica.muestra.Variante;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

// PDF mínimo de una página, sin librerías externas: alcanza para verificar la emisión del informe
@Component
public class GeneradorPdf {

    public byte[] generar(Muestra muestra) {
        List<String> lineas = new ArrayList<>();
        lineas.add("GenoClasifica - Informe de la muestra " + muestra.getCodigo());
        lineas.add("Corrida: " + muestra.getIdCorrida() + "   Recepcion: " + muestra.getFechaRecepcion());
        lineas.add("Q30: " + muestra.getQ30() + " %   Contaminacion: " + muestra.getContaminacion() + " %");
        lineas.add("Firmado por: " + (muestra.getFirmadaPor() == null ? "no requiere firma" : muestra.getFirmadaPor()));
        lineas.add("Variantes:");
        for (Variante v : muestra.getVariantes()) {
            String categoria = v.isRequiereRevision() ? "requiere revision" : String.valueOf(v.getCategoria());
            lineas.add(" - " + v.getGen() + " pos. " + v.getPosicion() + " (" + v.getTipo() + "): " + categoria
                + ", confianza " + v.getConfianza() + " %");
        }
        lineas.add("Datos sinteticos de prueba.");
        return armarPdf(lineas);
    }

    private byte[] armarPdf(List<String> lineas) {
        StringBuilder texto = new StringBuilder("BT /F1 11 Tf 50 790 Td 14 TL\n");
        for (String linea : lineas) {
            texto.append('(').append(escapar(linea)).append(") Tj T*\n");
        }
        texto.append("ET");

        List<String> objetos = List.of(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>",
            "<< /Length " + texto.length() + " >>\nstream\n" + texto + "\nendstream",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        List<Integer> posiciones = new ArrayList<>();
        escribir(pdf, "%PDF-1.4\n");
        for (int i = 0; i < objetos.size(); i++) {
            posiciones.add(pdf.size());
            escribir(pdf, (i + 1) + " 0 obj\n" + objetos.get(i) + "\nendobj\n");
        }
        int inicioXref = pdf.size();
        StringBuilder xref = new StringBuilder("xref\n0 " + (objetos.size() + 1) + "\n0000000000 65535 f \n");
        for (int posicion : posiciones) {
            xref.append(String.format("%010d 00000 n \n", posicion));
        }
        xref.append("trailer\n<< /Size ").append(objetos.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
            .append(inicioXref).append("\n%%EOF\n");
        escribir(pdf, xref.toString());
        return pdf.toByteArray();
    }

    // Helvetica básica no tiene tildes: se quitan y se escapan los paréntesis
    private String escapar(String linea) {
        String sinTildes = Normalizer.normalize(linea, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private void escribir(ByteArrayOutputStream salida, String texto) {
        salida.writeBytes(texto.getBytes(StandardCharsets.ISO_8859_1));
    }
}
