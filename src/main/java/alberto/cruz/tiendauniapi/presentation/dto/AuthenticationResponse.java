package alberto.cruz.tiendauniapi.presentation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

public record AuthenticationResponse(
        UUID id,
        String avatarUrl,
        String firstName,
        String lastName,
        boolean isVerified,

        @JsonIgnore
        String accessToken,
        @JsonIgnore
        String refreshToken
) {
}
