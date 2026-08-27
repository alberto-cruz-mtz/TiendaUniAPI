package alberto.cruz.tiendauniapi.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAvatarKeyRequest(

        @NotBlank(message = "La key del avatar no puede estar vacía")
        @Pattern(
                regexp = "^profiles/[0-9a-fA-F-]{36}/[0-9a-fA-F-]{36}\\.[a-zA-Z0-9]+$",
                message = "La key debe tener el formato profiles/<uuid>/<uuid>.<ext>"
        )
        String key
) {
}
