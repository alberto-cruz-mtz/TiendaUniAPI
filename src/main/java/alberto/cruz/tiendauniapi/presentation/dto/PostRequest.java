package alberto.cruz.tiendauniapi.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

public record PostRequest(
        @NotBlank(message = "El título no puede estar vacío")
        @Size(min = 4, max = 120, message = "El título debe tener entre 4 y 120 caracteres")
        String title,

        @NotBlank(message = "La descripción no puede estar vacía")
        @Size(max = 350, message = "La descripción no puede superar los 350 caracteres")
        String description,

        @NotEmpty(message = "El array de contenido multimedia no puede estar vacío")
        @Size(max = 10, message = "El array de contenido multimedia no puede tener más de 10 elementos")
        List<@Valid MediaContentRequest> mediaContent,

        @NotEmpty(message = "El array de productos no puede estar vacío")
        List<String> products,

        @Size(max = 10, message = "El array de tags no puede tener más de 10 elementos")
        List<String> tags,

        @NotNull(message = "El campo publishRightNow no puede ser nulo")
        Boolean publishRightNow,

        @NotNull(message = "La fecha de expiración es requerida")
        @Future(message = "La fecha de expiración debe ser una fecha futura a la fecha actual")
        Instant expirationDate
) {
}