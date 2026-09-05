package alberto.cruz.tiendauniapi.presentation.controller;

import alberto.cruz.tiendauniapi.persistence.model.AuthenticatedUser;
import alberto.cruz.tiendauniapi.presentation.dto.DataPaginationResponse;
import alberto.cruz.tiendauniapi.presentation.dto.OrderDetailResponse;
import alberto.cruz.tiendauniapi.presentation.dto.OrderRequest;
import alberto.cruz.tiendauniapi.presentation.dto.OrderResponse;
import alberto.cruz.tiendauniapi.presentation.dto.OrderSummaryResponse;
import alberto.cruz.tiendauniapi.presentation.dto.PostOrderDetail;
import alberto.cruz.tiendauniapi.service.interfaces.OrderService;
import alberto.cruz.tiendauniapi.service.model.ClientOrderKey;
import alberto.cruz.tiendauniapi.service.model.OrderId;
import alberto.cruz.tiendauniapi.service.model.PostId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("Idempotency-Key") String clientOrderId,
            @RequestBody @Valid OrderRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        UUID userId = authenticatedUser.getUserId();
        ClientOrderKey clientOrderKey = new ClientOrderKey(clientOrderId);
        OrderResponse response = orderService.createOrder(userId, clientOrderKey, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/orders/{id}")
                .buildAndExpand(response.orderId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(
            @PathVariable("orderId") String orderId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        UUID userId = authenticatedUser.getUserId();
        OrderId order = new OrderId(orderId);

        var orderDetail = orderService.getOrderById(order.value(), userId);
        return ResponseEntity.ok(orderDetail);
    }

    @GetMapping
    public ResponseEntity<DataPaginationResponse<OrderSummaryResponse>> getOrders(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID userId = authenticatedUser.getUserId();
        var orders = orderService.getOrdersByUserId(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<DataPaginationResponse<PostOrderDetail>> getOrdersByPostId(
            @PathVariable(name = "postId") String id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID userId = authenticatedUser.getUserId();
        PostId postId = new PostId(id);
        var orders = orderService.getOrdersByPostId(postId, pageable, userId);

        return ResponseEntity.ok(orders);
    }
}
