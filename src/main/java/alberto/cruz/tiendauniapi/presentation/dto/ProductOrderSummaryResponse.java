package alberto.cruz.tiendauniapi.presentation.dto;

import java.math.BigDecimal;

public record ProductOrderSummaryResponse(
        String photoUrl,
        BigDecimal quantity
) {
}
