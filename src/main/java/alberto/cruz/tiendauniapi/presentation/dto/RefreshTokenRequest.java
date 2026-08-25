package alberto.cruz.tiendauniapi.presentation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RefreshTokenRequest(
        @NotNull(message = "El ID de usuario no puede ser nulo")
        UUID userId
) {
}
