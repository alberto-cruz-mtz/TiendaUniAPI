package alberto.cruz.tiendauniapi.service.implementation;

import alberto.cruz.tiendauniapi.persistence.entity.OrderEntity;
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
import alberto.cruz.tiendauniapi.service.exception.InsufficientProductStockException;
import alberto.cruz.tiendauniapi.service.exception.OrderNotFoundException;
import alberto.cruz.tiendauniapi.service.exception.ProductNotFoundException;
import alberto.cruz.tiendauniapi.service.exception.ProductPriceChangedException;
import alberto.cruz.tiendauniapi.service.interfaces.OrderService;
import alberto.cruz.tiendauniapi.service.interfaces.UserService;
import alberto.cruz.tiendauniapi.service.model.ClientOrderKey;
import alberto.cruz.tiendauniapi.service.model.PostId;
import alberto.cruz.tiendauniapi.utils.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
        boolean isDuplicateRequest = orderEntity != null;
        if (isDuplicateRequest) {
            return OrderMapper.toOrderResponse(orderEntity);
        }

        UserEntity user = userService.getUserById(userId);
        PublicationEntity publication = this.findPublicationById(request.postId());
        List<ProductEntity> products = this.findProductsByIds(request.items());

        OrderEntity order = OrderMapper.toOrder(request, user, publication, clientOrderKey.value());
        List<ProductOrderEntity> orders = this.buildProductOrders(products, order, request.items());
        order.setProductOrders(orders);

        OrderEntity savedOrder = orderRepository.save(order);

        return new OrderResponse(savedOrder.getId(), savedOrder.getStatus(), savedOrder.getAmountPaid(), savedOrder.getPaymentMethod());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderById(UUID orderId, UUID userId) {
        OrderEntity order = this.findOrderByIdAndUserId(orderId, userId);

        List<ProductOrderDetailResponse> productOrdersDetail = OrderMapper.toProductOrderDetailResponse(order.getProductOrders());

        return OrderMapper.toOrderDetail(order, productOrdersDetail);
    }

    @Override
    @Transactional(readOnly = true)
    public DataPaginationResponse<OrderSummaryResponse> getOrdersByUserId(UUID userId, Pageable pageable) {
        Slice<OrderEntity> orders = orderRepository.findAllByUserId(userId, pageable);
        return this.buildDataOrderSummaryResponse(orders, pageable.getPageNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public DataPaginationResponse<PostOrderDetail> getOrdersByPostId(PostId postId, Pageable pageable, UUID userId) {
        Slice<OrderEntity> orders = orderRepository.findAllByPublicationIdAndUserId(postId.value(), userId, pageable);
        return this.buildDataPostOrderDetailResponse(orders, pageable.getPageNumber());
    }

    private static boolean isUnlimitedStock(BigDecimal quantity) {
        return quantity.compareTo(UNLIMITED_STOCK_MIN) >= 0
                && quantity.compareTo(BigDecimal.ZERO) < 0;
    }

    private PublicationEntity findPublicationById(String postId) {
        PostId publicationId = new PostId(postId);
        return publicationRepository.findById(publicationId.value())
                .orElseThrow(ProductNotFoundException::new);
    }

    private List<ProductEntity> findProductsByIds(List<ProductOrderItem> items) {
        List<UUID> productIds = items.stream()
                .map(ProductOrderItem::getProductIdAsUUID)
                .toList();

        var products = productRepository.findAllById(productIds);

        if (products.isEmpty()) {
            throw new ProductNotFoundException();
        }

        return products;
    }

    private List<ProductOrderEntity> buildProductOrders(List<ProductEntity> products, OrderEntity order, List<ProductOrderItem> productOrders) {
        Map<UUID, ProductEntity> productMap = products.stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));

        var productOrderList = productOrders.stream()
                .map(productOrder -> this.buildProductOrder(productMap, order, productOrder))
                .toList();

        productRepository.saveAll(productMap.values());

        return productOrderList;
    }

    private ProductOrderEntity buildProductOrder(Map<UUID, ProductEntity> productMap, OrderEntity order, ProductOrderItem productOrder) {
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

        return OrderMapper.toProductOrder(order, product, productOrder);
    }

    private OrderEntity findOrderByIdAndUserId(UUID orderId, UUID userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(OrderNotFoundException::new);
    }

    private DataPaginationResponse<OrderSummaryResponse> buildDataOrderSummaryResponse(Slice<OrderEntity> orders, int pageNumber) {
        boolean hasNext = orders.hasNext();
        String cursor = hasNext ? "/orders?page=" + (pageNumber + 1) : null;
        List<OrderSummaryResponse> orderList = OrderMapper.toOrderSummary(orders.getContent());

        return new DataPaginationResponse<>(orderList, cursor, hasNext);
    }

    private DataPaginationResponse<PostOrderDetail> buildDataPostOrderDetailResponse(Slice<OrderEntity> orders, int pageNumber) {
        boolean hasNext = orders.hasNext();
        String cursor = hasNext ? "/orders/posts?page=" + (pageNumber + 1) : null;

        List<PostOrderDetail> orderList = OrderMapper.toPostOrderDetail(orders.getContent());

        return new DataPaginationResponse<>(orderList, cursor, hasNext);
    }
}
