package alberto.cruz.tiendauniapi.presentation.advice;

import alberto.cruz.tiendauniapi.service.exception.PresignedUrlGenerationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class PresignedUrlExceptionHandlerTest {

    private static final URI EXPECTED_TYPE =
            URI.create("https://tiendauniapi.com/problems/presigned-url-generation-failed");

    @Test
    void handlePresignedUrlGenerationException_returns503WithExpectedProblemDetail() {
        PresignedUrlExceptionHandler handler = new PresignedUrlExceptionHandler();

        ResponseEntity<ProblemDetail> response = handler.handlePresignedUrlGenerationException(
                new PresignedUrlGenerationException("Failed to generate presigned URL",
                        new RuntimeException("aws down")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(body.getType()).isEqualTo(EXPECTED_TYPE);
        assertThat(body.getTitle()).isEqualTo("Presigned Url Generation Failed");
        assertThat(body.getDetail()).contains("No se pudo generar la URL pre-firmada");
    }
}