package alberto.cruz.tiendauniapi.presentation.advice;

import alberto.cruz.tiendauniapi.service.exception.PresignedUrlGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Maps failures from the AWS SDK presigner (wrapped in
 * {@link PresignedUrlGenerationException}) to a stable 503 ProblemDetail so the
 * client can distinguish a transient dependency outage from a real bug.
 *
 * <p>Lower precedence than {@link GlobalExceptionHandler} on purpose: the latter
 * already owns validation and infrastructure errors at {@code HIGHEST_PRECEDENCE}.
 * This advice only needs to catch its own exception type.
 */
@Slf4j
@Order(20)
@RestControllerAdvice
public class PresignedUrlExceptionHandler {

    private static final URI TYPE = URI.create(GlobalExceptionHandler.DOMAIN_URI + "/presigned-url-generation-failed");
    private static final String TITLE = "Presigned Url Generation Failed";
    private static final String DETAIL = "No se pudo generar la URL pre-firmada para subir el archivo. Intenta nuevamente en unos momentos.";

    @ExceptionHandler(PresignedUrlGenerationException.class)
    public ResponseEntity<ProblemDetail> handlePresignedUrlGenerationException(PresignedUrlGenerationException ex) {
        log.error("Failed to generate presigned URL", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, DETAIL);
        problemDetail.setType(TYPE);
        problemDetail.setTitle(TITLE);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problemDetail);
    }
}