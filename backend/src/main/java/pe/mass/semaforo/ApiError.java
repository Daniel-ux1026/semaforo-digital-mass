package pe.mass.semaforo;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
class ApiError {
 @ExceptionHandler(IllegalArgumentException.class)
 ResponseEntity<?> invalid(IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("message",e.getMessage())); }
 @ExceptionHandler({MethodArgumentNotValidException.class,org.springframework.http.converter.HttpMessageNotReadableException.class})
 ResponseEntity<?> validation(Exception e) { return ResponseEntity.badRequest().body(Map.of("message","Revise los campos enviados.")); }
 @ExceptionHandler(DataIntegrityViolationException.class)
 ResponseEntity<?> conflict(Exception e) { return ResponseEntity.status(409).body(Map.of("message","El registro ya existe o incumple una restricción.")); }
 @ExceptionHandler(ResponseStatusException.class)
 ResponseEntity<?> status(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null ? "Operación rechazada." : e.getReason())); }
}
