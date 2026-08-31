package alberto.cruz.tiendauniapi.presentation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotEmpty(message = "El nombre es requerido")
        @Size(max = 70, message = "El nombre no puede tener más de 70 caracteres")
        String name,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "-1.0", message = "La cantidad debe ser mayor a cero")
        @Digits(integer = 8, fraction = 2, message = "Formato inválido. Máximo 8 enteros y 2 decimales")
        BigDecimal quantity,

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio de venta debe ser mayor a cero")
        @Digits(integer = 8, fraction = 2, message = "Formato inválido. Máximo 8 enteros y 2 decimales")
        BigDecimal salePrice,

        @NotBlank(message = "La categoría es requerida")
        @Pattern(
                regexp = "(?i)^(food_and_drinks|electronics|clothing|home_and_garden|health_and_beauty|sports_and_outdoors|sweets_and_snacks|toys_and_games|others)$",
                message = "La categoría debe ser una de las siguientes: 'food_and_drinks', 'electronics', 'clothing', 'home_and_garden', 'health_and_beauty', 'sports_and_outdoors', 'sweets_and_snacks', 'toys_and_games', 'others'"
        )
        String category,

        @NotBlank(message = "El tipo de venta es requerido")
        @Pattern(regexp = "(?i)^(pre_order|sale_on_delivery)$", message = "El tipo de venta debe ser 'pre_order' o 'sale_on_delivery'")
        String saleType,

        @Size(max = 300, message = "La URL de la foto no puede tener más de 300 caracteres")
        String photoUrl
) {
}
