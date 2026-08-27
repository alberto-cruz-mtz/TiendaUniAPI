package alberto.cruz.tiendauniapi.presentation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record TokenBundle(
        String accessToken,
        String refreshToken
) {
}
