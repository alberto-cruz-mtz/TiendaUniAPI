package alberto.cruz.tiendauniapi.presentation.dto;

import alberto.cruz.tiendauniapi.presentation.validation.UniqueFileIds;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@UniqueFileIds
public record PresignedUrlPublicationRequest(

        @NotEmpty(message = "El array de archivos no puede estar vacío")
        @Size(max = 10, message = "El array de archivos no puede tener más de 10 elementos")
        @Valid
        List<PresignedUrlItem> files
) {
}
