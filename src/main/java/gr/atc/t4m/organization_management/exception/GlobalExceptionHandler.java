package gr.atc.t4m.organization_management.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice

public class GlobalExceptionHandler {
    private static final String ERROR_MESSAGE = "error_message";

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, String> error = new HashMap<>();
        error.put(ERROR_MESSAGE, ex.getReason());
        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    @ExceptionHandler(OrganizationAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateOrganization(OrganizationAlreadyExistsException ex) {
        Map<String, String> error = new HashMap<>();
        error.put(ERROR_MESSAGE, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(OrganizationNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFoundOrganization(OrganizationNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put(ERROR_MESSAGE, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
}
