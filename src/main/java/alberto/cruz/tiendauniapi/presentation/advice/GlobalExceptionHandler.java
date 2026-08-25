package alberto.cruz.tiendauniapi.presentation.advice;

import alberto.cruz.tiendauniapi.common.UnknownException;
import alberto.cruz.tiendauniapi.presentation.dto.IncorrectField;
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

import java.net.URI;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    public static final String DOMAIN_URI = "https://tiendauniapi.com/problems";

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

        String message = "Uno o más campos no cumplen con las reglas de validación.";

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
        problemDetail.setType(URI.create(DOMAIN_URI + "/validations"));
        problemDetail.setTitle("Validation Failed");
        problemDetail.setProperty("errors", fieldErrors);

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
}
