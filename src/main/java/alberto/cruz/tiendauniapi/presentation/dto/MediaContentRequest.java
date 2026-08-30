package alberto.cruz.tiendauniapi.presentation.dto;

import alberto.cruz.tiendauniapi.persistence.entity.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MediaContentRequest(
        @NotNull(message = "El tipo de medio no puede ser nulo")
        MediaType mediaType,

        @NotBlank(message = "La clave del medio no puede estar vacía")
        @Size(max = 300, message = "La clave del medio no puede superar los 300 caracteres")
        String mediaKey,

        @NotNull(message = "El número de orden no puede ser nulo")
        @PositiveOrZero(message = "El número de orden debe ser mayor o igual a cero")
        Integer orderNumber
) {
}