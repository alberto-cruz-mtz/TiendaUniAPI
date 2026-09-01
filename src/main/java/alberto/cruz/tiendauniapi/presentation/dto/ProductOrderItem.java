package alberto.cruz.tiendauniapi.presentation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductOrderItem(
        @NotEmpty(message = "El ID del producto es requerido")
        @Pattern(
                regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "El ID proporcionado no tiene un formato UUID válido"
        )
        String productId,

        @NotNull(message = "La cantidad es requerida")
        @DecimalMin(value = "0.0", inclusive = false, message = "La cantidad debe ser mayor que cero")
        BigDecimal quantity,

        @NotNull(message = "El precio es requerido")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor que cero")
        BigDecimal price
) {
        public UUID getProductIdAsUUID() {
                return UUID.fromString(this.productId);
        }
}
