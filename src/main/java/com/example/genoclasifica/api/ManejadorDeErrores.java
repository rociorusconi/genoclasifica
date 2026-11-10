package com.example.genoclasifica.api;

import com.example.genoclasifica.muestra.FirmaRequeridaException;
import com.example.genoclasifica.muestra.MuestraNoEncontradaException;
import com.example.genoclasifica.muestra.PermisoDenegadoException;
import com.example.genoclasifica.muestra.TransicionInvalidaException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

// Convierte cada error del sistema en un código HTTP con un mensaje en JSON
@RestControllerAdvice
public class ManejadorDeErrores {

    @ExceptionHandler(MuestraNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> noEncontrada(MuestraNoEncontradaException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // Va antes que TransicionInvalida: la falta de firma es 403, como pide CP-ST-INV-01
    @ExceptionHandler(FirmaRequeridaException.class)
    public ResponseEntity<Map<String, Object>> firmaRequerida(FirmaRequeridaException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(PermisoDenegadoException.class)
    public ResponseEntity<Map<String, Object>> permisoDenegado(PermisoDenegadoException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(TransicionInvalidaException.class)
    public ResponseEntity<Map<String, Object>> transicionInvalida(TransicionInvalidaException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> datosInvalidos(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "Datos inválidos: " + detalle);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonIlegible(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo del pedido no es un JSON válido");
    }

    // Por ejemplo ?categoria=OTRA o ?desde=ayer en los filtros del portal
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "Valor inválido para el filtro " + e.getName() + ": " + e.getValue());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> valorInvalido(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> codigoRepetido(DataIntegrityViolationException e) {
        return error(HttpStatus.CONFLICT, "Ya existe una muestra con ese código");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("status", status.value());
        cuerpo.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
