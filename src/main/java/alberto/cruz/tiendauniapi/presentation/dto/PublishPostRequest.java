package alberto.cruz.tiendauniapi.presentation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record PublishPostRequest(
        @NotNull
        @Future(message = "La fecha de expiración debe ser una fecha futura a la fecha actual")
        Instant expirationDate
) {
}
