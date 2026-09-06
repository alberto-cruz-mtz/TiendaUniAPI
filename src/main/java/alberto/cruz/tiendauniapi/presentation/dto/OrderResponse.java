package alberto.cruz.tiendauniapi.presentation.dto;

import alberto.cruz.tiendauniapi.persistence.entity.OrderStatus;
import alberto.cruz.tiendauniapi.persistence.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        OrderStatus status,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod
) {
}
