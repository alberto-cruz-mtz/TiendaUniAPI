package alberto.cruz.tiendauniapi.presentation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RegisterResponse(
        String email,
        String firstName,
        String lastName,
        String avatarUrl,
        boolean isVerified,

        @JsonIgnore
        TokenBundle tokenBundle
) {
}
