package alberto.cruz.tiendauniapi.presentation.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostDetailResponse(
        UUID id,
        String title,
        String description,
        List<MediaContentRequest> mediaContent,
        List<ProductItem> products,
        Instant postedAt,
        Instant expirationDate,
        boolean isPublished
) {
}
