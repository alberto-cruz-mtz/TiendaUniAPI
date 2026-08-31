package alberto.cruz.tiendauniapi.presentation.dto;

import alberto.cruz.tiendauniapi.persistence.entity.SaleType;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductItem(
        UUID id,
        String name,
        BigDecimal quantity,
        BigDecimal salePrice,
        String category,
        SaleType saleType,
        String photoUrl
) {
}
