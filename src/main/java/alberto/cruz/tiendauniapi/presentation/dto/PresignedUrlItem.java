package alberto.cruz.tiendauniapi.presentation.dto;

import alberto.cruz.tiendauniapi.service.model.PresignedUrlMimeExtension;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PresignedUrlItem(

        @NotBlank(message = "El id del archivo no puede estar vacío")
        String id,

        @NotBlank(message = "El nombre del archivo no puede estar vacío")
        @Pattern(
                regexp = "^[a-zA-Z0-9_-]+$",
                message = "El nombre del archivo sólo puede contener letras, dígitos, guion y guion bajo"
        )
        @Size(max = 255, message = "El nombre del archivo no puede superar los 255 caracteres")
        String fileName,

        @Positive(message = "El tamaño del archivo debe ser mayor a cero")
        Long size,

        @NotBlank(message = "El mimeType no puede estar vacío")
        @Pattern(
                regexp = PresignedUrlMimeExtension.WHITELIST_REGEX,
                flags = {Pattern.Flag.CASE_INSENSITIVE},
                message = "El mimeType no está dentro de la whitelist permitida"
        )
        String mimeType
) {
}
