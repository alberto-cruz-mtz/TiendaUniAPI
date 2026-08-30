package alberto.cruz.tiendauniapi.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record StatusPostRequest(
        @NotBlank(message = "El estado no puede estar vacío")
        @Pattern(regexp = "^(expired|hidden|published)$", message = "El estado debe ser 'expired' o 'hidden'")
        String status
) {
}
