package alberto.cruz.tiendauniapi.service.implementation;

import alberto.cruz.tiendauniapi.persistence.entity.OrderEntity;
import alberto.cruz.tiendauniapi.persistence.entity.OrderStatus;
import alberto.cruz.tiendauniapi.persistence.entity.PaymentMethod;
import alberto.cruz.tiendauniapi.persistence.entity.ProductEntity;
import alberto.cruz.tiendauniapi.persistence.entity.ProductOrderEntity;
import alberto.cruz.tiendauniapi.persistence.entity.PublicationEntity;
import alberto.cruz.tiendauniapi.persistence.entity.UserEntity;
import alberto.cruz.tiendauniapi.persistence.repository.OrderRepository;
import alberto.cruz.tiendauniapi.persistence.repository.ProductRepository;
import alberto.cruz.tiendauniapi.persistence.repository.PublicationRepository;
import alberto.cruz.tiendauniapi.presentation.dto.DataPaginationResponse;
import alberto.cruz.tiendauniapi.presentation.dto.OrderRequest;
import alberto.cruz.tiendauniapi.presentation.dto.OrderResponse;
import alberto.cruz.tiendauniapi.presentation.dto.OrderDetailResponse;
import alberto.cruz.tiendauniapi.presentation.dto.OrderSummaryResponse;
import alberto.cruz.tiendauniapi.presentation.dto.PostOrderDetail;
import alberto.cruz.tiendauniapi.presentation.dto.ProductOrderDetailResponse;
import alberto.cruz.tiendauniapi.presentation.dto.ProductOrderItem;
import alberto.cruz.tiendauniapi.presentation.dto.ProductOrderSummaryResponse;
import alberto.cruz.tiendauniapi.presentation.dto.UserSummary;
import alberto.cruz.tiendauniapi.service.exception.InsufficientProductStockException;
import alberto.cruz.tiendauniapi.service.exception.OrderNotFoundException;
import alberto.cruz.tiendauniapi.service.exception.ProductNotFoundException;
import alberto.cruz.tiendauniapi.service.exception.ProductPriceChangedException;
import alberto.cruz.tiendauniapi.service.interfaces.OrderService;
import alberto.cruz.tiendauniapi.service.interfaces.UserService;
import alberto.cruz.tiendauniapi.service.model.ClientOrderKey;
import alberto.cruz.tiendauniapi.service.model.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal UNLIMITED_STOCK_MIN = new BigDecimal("-1");

    private final UserService userService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PublicationRepository publicationRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(UUID userId, ClientOrderKey clientOrderKey, OrderRequest request) {
        OrderEntity orderEntity = orderRepository.findByClientKey(clientOrderKey.value()).orElse(null);
        if (orderEntity != null) {
            return new OrderResponse(orderEntity.getId(), orderEntity.getStatus(), orderEntity.getAmountPaid(), orderEntity.getPaymentMethod());
        }

        UserEntity user = userService.getUserById(userId);
        List<UUID> productIds = request.items().stream()
                .map(ProductOrderItem::getProductIdAsUUID)
                .toList();

        PostId postId = new PostId(request.postId());
        PublicationEntity publication = publicationRepository.findById(postId.value())
                .orElseThrow(ProductNotFoundException::new);

        List<ProductEntity> products = productRepository.findAllById(productIds);

        if (products.isEmpty()) {
            throw new ProductNotFoundException();
        }

        PaymentMethod paymentMethod = PaymentMethod.valueOf(request.paymentMethod().toUpperCase());
        OrderStatus status = switch (paymentMethod) {
            case CASH, BANK_CARD -> OrderStatus.PENDING_PAYMENT;
            case TRANSFER -> {
                if (request.paymentProof() == null || request.paymentProof().isBlank()) {
                    yield OrderStatus.PENDING_PAYMENT;
                }
                yield OrderStatus.PENDING_PROOF_VERIFICATION;
            }
        };

        OrderEntity order = OrderEntity.builder()
                .user(user)
                .clientKey(clientOrderKey.value())
                .paymentMethod(paymentMethod)
                .amountPaid(request.totalAmount())
                .paymentProofUrl(request.paymentProof())
                .status(status)
                .publication(publication)
                .build();

        Map<UUID, ProductEntity> productMap = products.stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));

        var orders = request.items().stream()
                .map(productOrder -> {
                    ProductEntity product = productMap.get(productOrder.getProductIdAsUUID());

                    if (product == null) {
                        throw new ProductNotFoundException();
                    }

                    if (!isUnlimitedStock(product.getQuantity())) {
                        if (product.getQuantity().compareTo(productOrder.quantity()) < 0) {
                            throw new InsufficientProductStockException(product.getName());
                        }

                        product.setQuantity(product.getQuantity().subtract(productOrder.quantity()));
                    }

                    if (product.getSalePrice().compareTo(productOrder.price()) != 0) {
                        throw new ProductPriceChangedException(product.getName());
                    }

                    return ProductOrderEntity.builder()
                            .order(order)
                            .product(product)
                            .quantity(productOrder.quantity())
                            .unitPrice(productOrder.price())
                            .build();
                })
                .toList();

        order.setProductOrders(orders);
        OrderEntity savedOrder = orderRepository.save(order);
        productRepository.saveAll(productMap.values());

        return new OrderResponse(savedOrder.getId(), savedOrder.getStatus(), savedOrder.getAmountPaid(), savedOrder.getPaymentMethod());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderById(UUID orderId, UUID userId) {
        OrderEntity order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(OrderNotFoundException::new);

        List<ProductOrderDetailResponse> productOrdersDetail = order.getProductOrders().stream()
                .map(productOrder -> {
                    ProductEntity product = productOrder.getProduct();

                    return new ProductOrderDetailResponse(
                            product.getPhotoUrl(),
                            product.getName(),
                            productOrder.getQuantity(),
                            productOrder.getUnitPrice()
                    );
                })
                .toList();

        return new OrderDetailResponse(
                order.getId(),
                order.getPublication().getId(),
                order.getStatus(),
                order.getAmountPaid(),
                order.getPaymentMethod(),
                productOrdersDetail
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DataPaginationResponse<OrderSummaryResponse> getOrdersByUserId(UUID userId, Pageable pageable) {
        Slice<OrderEntity> orders = orderRepository.findAllByUserId(userId, pageable);

        boolean hasNext = orders.hasNext();
        String cursor = hasNext ? "/orders?page=" + (pageable.getPageNumber() + 1) : null;
        List<OrderSummaryResponse> orderList = orders.stream()
                .map(order -> {
                    List<ProductOrderSummaryResponse> productOrderSummaries = order.getProductOrders().stream()
                            .map(productOrder -> new ProductOrderSummaryResponse(
                                    productOrder.getProduct().getPhotoUrl(),
                                    productOrder.getQuantity()
                            ))
                            .toList();

                    return new OrderSummaryResponse(
                            order.getId(),
                            order.getStatus(),
                            order.getAmountPaid(),
                            order.getPaymentMethod(),
                            productOrderSummaries
                    );
                })
                .toList();

        return new DataPaginationResponse<>(orderList, cursor, hasNext);
    }

    @Override
    @Transactional(readOnly = true)
    public DataPaginationResponse<PostOrderDetail> getOrdersByPostId(PostId postId, Pageable pageable, UUID userId) {
        Slice<OrderEntity> orders = orderRepository.findAllByPublicationIdAndUserId(postId.value(), userId, pageable);

        boolean hasNext = orders.hasNext();
        String cursor = hasNext ? "/orders/posts?page=" + (pageable.getPageNumber() + 1) : null;

        List<PostOrderDetail> orderList = orders.stream()
                .map(order -> {
                    List<ProductOrderDetailResponse> productOrderSummaries = order.getProductOrders().stream()
                            .map(productOrder -> new ProductOrderDetailResponse(
                                    productOrder.getProduct().getPhotoUrl(),
                                    productOrder.getProduct().getName(),
                                    productOrder.getQuantity(),
                                    productOrder.getUnitPrice()
                            ))
                            .toList();

                    String fullName = order.getUser().getFirstName() + " " + order.getUser().getLastName();
                    UserSummary user = new UserSummary(order.getUser().getAvatarUrl(), fullName);

                    return new PostOrderDetail(
                            order.getId(),
                            user,
                            order.getStatus(),
                            order.getAmountPaid(),
                            order.getPaymentMethod(),
                            productOrderSummaries,
                            order.getCreatedAt()
                    );
                }).toList();

        return new DataPaginationResponse<>(orderList, cursor, hasNext);
    }

    private static boolean isUnlimitedStock(BigDecimal quantity) {
        return quantity.compareTo(UNLIMITED_STOCK_MIN) >= 0
                && quantity.compareTo(BigDecimal.ZERO) < 0;
    }
}
