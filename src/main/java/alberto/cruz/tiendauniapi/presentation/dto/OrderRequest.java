package alberto.cruz.tiendauniapi.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
        List<@Valid ProductOrderItem> items,

        @NotBlank(message = "El ID de la publicación es requerido")
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "El ID de la publicación debe ser un UUID válido")
        String postId
) {
}
