package alberto.cruz.tiendauniapi.presentation.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductItem(
        UUID id,
        String name,
        BigDecimal quantity,
        BigDecimal salePrice,
        String categoryName,
        String photoUrl
) {
}
