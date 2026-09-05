package alberto.cruz.tiendauniapi.service.interfaces;

import alberto.cruz.tiendauniapi.presentation.dto.DataPaginationResponse;
import alberto.cruz.tiendauniapi.presentation.dto.OrderRequest;
import alberto.cruz.tiendauniapi.presentation.dto.OrderResponse;
import alberto.cruz.tiendauniapi.presentation.dto.OrderDetailResponse;
import alberto.cruz.tiendauniapi.presentation.dto.OrderSummaryResponse;
import alberto.cruz.tiendauniapi.presentation.dto.PostOrderDetail;
import alberto.cruz.tiendauniapi.service.model.ClientOrderKey;
import alberto.cruz.tiendauniapi.service.model.PostId;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(UUID userId, ClientOrderKey clientOrderKey, OrderRequest request);

    OrderDetailResponse getOrderById(UUID orderId, UUID userId);

    DataPaginationResponse<OrderSummaryResponse> getOrdersByUserId(UUID userId, Pageable pageable);

    DataPaginationResponse<PostOrderDetail> getOrdersByPostId(PostId postId, Pageable pageable, UUID userId);
}
