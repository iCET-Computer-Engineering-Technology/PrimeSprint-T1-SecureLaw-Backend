package com.primesprint.exception;

import com.google.genai.errors.ClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClientException.class)
    public ResponseEntity<Map<String, Object>> handleGenerativeClientException(ClientException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("429 Too Many Requests")) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Rate Limit Exceeded", "message", "You have exceeded your AI provider quota. Please try again later."));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "AI Service Error", "message", ex.getMessage()));
    }
}

