package edu.srmist.eventreg.web;

import io.camunda.client.api.command.ClientException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns exceptions into a consistent JSON error body the React portal can show to the user. */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String, Object>> handleApi(ApiException e) {
    return body(e.status(), e.getMessage(), List.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
    List<String> details = e.getBindingResult().getFieldErrors().stream()
        .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
    return body(HttpStatus.BAD_REQUEST, "Some fields are invalid", details);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException e) {
    return body(HttpStatus.BAD_REQUEST, "The request body could not be read", List.of());
  }

  @ExceptionHandler(ClientException.class)
  public ResponseEntity<Map<String, Object>> handleCamunda(ClientException e) {
    log.warn("Camunda request failed: {}", e.getMessage());
    return body(HttpStatus.BAD_GATEWAY, "The workflow engine rejected the request: " + e.getMessage(), List.of());
  }

  private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String message, List<String> details) {
    Map<String, Object> b = new LinkedHashMap<>();
    b.put("status", status.value());
    b.put("error", status.getReasonPhrase());
    b.put("message", message);
    b.put("details", details);
    b.put("timestamp", OffsetDateTime.now());
    return ResponseEntity.status(status).body(b);
  }
}
