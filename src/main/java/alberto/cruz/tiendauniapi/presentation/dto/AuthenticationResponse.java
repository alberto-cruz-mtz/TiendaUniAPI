package alberto.cruz.tiendauniapi.presentation.dto;

import java.util.UUID;

public record AuthenticationResponse(
        UUID id,
        String avatarUrl,
        String firstName,
        String lastName,
        boolean isVerified
) {
}
