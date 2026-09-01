package alberto.cruz.tiendauniapi.service.interfaces;

import alberto.cruz.tiendauniapi.presentation.dto.OrderRequest;
import alberto.cruz.tiendauniapi.presentation.dto.OrderResponse;
import alberto.cruz.tiendauniapi.presentation.dto.OrderDetailResponse;
import alberto.cruz.tiendauniapi.presentation.dto.OrderSummaryResponse;
import alberto.cruz.tiendauniapi.service.model.ClientOrderKey;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(UUID userId, ClientOrderKey clientOrderKey, OrderRequest request);

    OrderDetailResponse getOrderById(UUID orderId, UUID userId);

    List<OrderSummaryResponse> getOrdersByUserId(UUID userId);

}
