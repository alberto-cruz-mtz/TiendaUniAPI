package alberto.cruz.tiendauniapi.presentation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record RegisterResponse(
        String email,
        String firstName,
        String lastName,
        boolean isVerified,

        @JsonIgnore
        String accessToken,
        @JsonIgnore
        String refreshToken
) {
}
