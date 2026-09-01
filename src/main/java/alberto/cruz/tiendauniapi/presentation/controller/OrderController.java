package alberto.cruz.tiendauniapi.presentation.controller;

import alberto.cruz.tiendauniapi.persistence.model.AuthenticatedUser;
import alberto.cruz.tiendauniapi.presentation.dto.OrderRequest;
import alberto.cruz.tiendauniapi.presentation.dto.OrderResponse;
import alberto.cruz.tiendauniapi.service.interfaces.OrderService;
import alberto.cruz.tiendauniapi.service.model.ClientOrderKey;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
}
