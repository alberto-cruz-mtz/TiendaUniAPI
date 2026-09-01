package alberto.cruz.tiendauniapi.presentation.dto;

import java.math.BigDecimal;

public record ProductOrderDetailResponse(
        String photoUrl,
        String name,
        BigDecimal quantity,
        BigDecimal price
) {
}
