package alberto.cruz.tiendauniapi.presentation.advice;

import alberto.cruz.tiendauniapi.common.UnknownException;
import alberto.cruz.tiendauniapi.presentation.dto.IncorrectField;
import alberto.cruz.tiendauniapi.service.exception.InvalidArgumentException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedRuntimeException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    public static final String DOMAIN_URI = "https://tiendauniapi.com/problems";

    private static final String GENERIC_VALIDATION_DETAIL = "Uno o más campos no cumplen con las reglas de validación.";

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle("Acceso Denegado");
        problemDetail.setType(URI.create(DOMAIN_URI + "/forbidden"));
        problemDetail.setProperty("reason", "No tienes los permisos o roles necesarios para acceder a este recurso.");
        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler(UnknownException.class)
    public ResponseEntity<ProblemDetail> handleUnknownException(UnknownException exception) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        log.error("An unknown exception occurred", exception);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, "Ocurrió un error inesperado. Por favor, intente nuevamente más tarde.");
        problemDetail.setTitle("Unknown Exception");
        problemDetail.setType(URI.create(DOMAIN_URI + "/unknown-exception"));
        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST; // 400

        Map<String, IncorrectField> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new IncorrectField(fieldError.getField(), fieldError.getDefaultMessage()))
                .collect(Collectors.toMap(IncorrectField::field, Function.identity(), (existing, replacement) -> existing));

        String globalErrorMessage = ex.getBindingResult().getGlobalErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? GENERIC_VALIDATION_DETAIL : error.getDefaultMessage())
                .orElse(null);

        String message = fieldErrors.isEmpty() && globalErrorMessage != null ? globalErrorMessage : GENERIC_VALIDATION_DETAIL;

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
        problemDetail.setType(URI.create(DOMAIN_URI + "/validations"));
        problemDetail.setTitle("Validation Failed");
        problemDetail.setProperty("errors", fieldErrors);

        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(ConstraintViolationException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage() == null ? GENERIC_VALIDATION_DETAIL : violation.getMessage())
                .orElse(GENERIC_VALIDATION_DETAIL);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
        problemDetail.setType(URI.create(DOMAIN_URI + "/validations"));
        problemDetail.setTitle("Validation Failed");

        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, GENERIC_VALIDATION_DETAIL);
        problemDetail.setType(URI.create(DOMAIN_URI + "/invalid-argument"));
        problemDetail.setTitle("Invalid Argument");
        problemDetail.setProperty("errors", new IncorrectField("argument", ex.getMessage()));

        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler({DataIntegrityViolationException.class, DataAccessException.class})
    public ResponseEntity<ProblemDetail> handleNestedRuntimeException(NestedRuntimeException exception) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        log.error("An data integrity violation exception occurred", exception);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, "Ocurrió un error inesperado. Por favor, intente nuevamente más tarde.");
        problemDetail.setTitle("Unknown Exception");
        problemDetail.setType(URI.create(DOMAIN_URI + "/unknown-exception"));

        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler(InvalidArgumentException.class)
    public ResponseEntity<ProblemDetail> handleInvalidArgumentException(InvalidArgumentException exception) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problemDetail.setType(URI.create(DOMAIN_URI + "/validations"));
        problemDetail.setTitle("Validation Failed");

        IncorrectField incorrectField = new IncorrectField(exception.getArgumentName(), exception.getMessage());
        problemDetail.setProperty("errors", incorrectField);

        return ResponseEntity.status(status).body(problemDetail);
    }
}