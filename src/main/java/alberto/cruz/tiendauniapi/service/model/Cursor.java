package alberto.cruz.tiendauniapi.service.model;

import java.time.Instant;
import java.util.UUID;

public record Cursor(UUID postId, Instant postedAt) {
}
