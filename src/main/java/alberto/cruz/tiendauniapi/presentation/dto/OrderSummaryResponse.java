package alberto.cruz.tiendauniapi.presentation.dto;

import alberto.cruz.tiendauniapi.persistence.entity.OrderStatus;
import alberto.cruz.tiendauniapi.persistence.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID orderId,
        OrderStatus status,
        BigDecimal amountPaid,
        PaymentMethod paymentMethod,
        List<ProductOrderSummaryResponse> products
) {
}
