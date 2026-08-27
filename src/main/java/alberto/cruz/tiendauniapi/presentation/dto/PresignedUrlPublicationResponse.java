package alberto.cruz.tiendauniapi.presentation.dto;

import java.util.List;

public record PresignedUrlPublicationResponse(List<PresignedUrlItemResponse> uris) {
}
