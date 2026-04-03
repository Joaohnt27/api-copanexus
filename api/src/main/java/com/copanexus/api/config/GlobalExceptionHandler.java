package com.copanexus.api.config;

import com.copanexus.api.dto.ErroResposta;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Trata erros de "Não Encontrado" (404)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErroResposta> handleRuntime(RuntimeException ex) {
        ErroResposta erro = new ErroResposta(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(erro, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Trata erros de validação ou argumentos inválidos (400)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResposta> handleBadRequest(IllegalArgumentException ex) {
        ErroResposta erro = new ErroResposta(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(erro, HttpStatus.BAD_REQUEST);
    }
}