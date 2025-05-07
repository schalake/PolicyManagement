package com.eana.insurance.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class ExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("timestamp", Instant.now().toString());
        responseBody.put("status", status.value());
        responseBody.put("error", status.value());
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            responseBody.put(fieldName, message);
        });
        return new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);
    }

    // Handle JSON parse errors (like invalid date format)
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String message = "Malformed request";
        if (ex.getCause() instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException) {
            message = "Invalid input format. For dates, use dd-MM-yyyy.";
        }

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("timestamp", Instant.now().toString());
        responseBody.put("status", status.value());
        responseBody.put("error", status.value());
        responseBody.put("errors", message);

        return new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);
    }
}