package alberto.cruz.tiendauniapi.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record OrderRequest(
        @NotEmpty(message = "El método de pago es requerido")
        @Pattern(regexp = "(?i)^(cash|transfer|bank_card)$", message = "El método de pago debe ser 'cash', 'transfer' o 'bank_card'")
        String paymentMethod,

        @NotNull(message = "El monto total es requerido")
        @DecimalMin(value = "0.0", message = "El monto total debe ser mayor que cero")
        BigDecimal totalAmount,

        @Size(max = 300, message = "La prueba de pago no puede exceder los 255 caracteres")
        String paymentProof,

        @NotEmpty(message = "La lista de productos no puede estar vacía")
        @Size(min = 1, message = "Debe haber al menos un producto en la orden")
        List<@Valid ProductOrderItem> items
) {
}
