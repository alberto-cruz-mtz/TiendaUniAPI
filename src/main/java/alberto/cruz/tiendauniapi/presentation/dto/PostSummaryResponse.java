package alberto.cruz.tiendauniapi.presentation.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostSummaryResponse(
        UUID id,
        String title,
        String description,
        List<MediaContentRequest> mediaContent,
        Instant postedAt,
        String status
) {
}
