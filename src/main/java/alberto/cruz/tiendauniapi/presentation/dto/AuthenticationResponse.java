package alberto.cruz.tiendauniapi.presentation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthenticationResponse(
        UUID id,
        String avatarUrl,
        String firstName,
        String lastName,
        boolean isVerified,

        @JsonIgnore
        TokenBundle tokenBundle
) {
}
