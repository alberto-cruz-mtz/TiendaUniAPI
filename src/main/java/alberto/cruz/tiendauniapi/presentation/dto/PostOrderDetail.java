package alberto.cruz.tiendauniapi.presentation.dto;

import alberto.cruz.tiendauniapi.persistence.entity.OrderStatus;
import alberto.cruz.tiendauniapi.persistence.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostOrderDetail(
        UUID orderId,
        UserSummary user,
        OrderStatus status,
        BigDecimal amountPaid,
        PaymentMethod paymentMethod,
        List<ProductOrderDetailResponse> products,
        Instant orderDate
) {
}
