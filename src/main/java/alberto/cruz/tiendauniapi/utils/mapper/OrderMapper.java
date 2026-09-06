package alberto.cruz.tiendauniapi.utils.mapper;

import alberto.cruz.tiendauniapi.persistence.entity.OrderEntity;
import alberto.cruz.tiendauniapi.persistence.entity.OrderStatus;
import alberto.cruz.tiendauniapi.persistence.entity.PaymentMethod;
import alberto.cruz.tiendauniapi.persistence.entity.ProductEntity;
import alberto.cruz.tiendauniapi.persistence.entity.ProductOrderEntity;
import alberto.cruz.tiendauniapi.persistence.entity.PublicationEntity;
import alberto.cruz.tiendauniapi.persistence.entity.UserEntity;
import alberto.cruz.tiendauniapi.presentation.dto.OrderDetailResponse;
import alberto.cruz.tiendauniapi.presentation.dto.OrderRequest;
import alberto.cruz.tiendauniapi.presentation.dto.OrderResponse;
import alberto.cruz.tiendauniapi.presentation.dto.OrderSummaryResponse;
import alberto.cruz.tiendauniapi.presentation.dto.PostOrderDetail;
import alberto.cruz.tiendauniapi.presentation.dto.ProductOrderDetailResponse;
import alberto.cruz.tiendauniapi.presentation.dto.ProductOrderItem;
import alberto.cruz.tiendauniapi.presentation.dto.ProductOrderSummaryResponse;
import alberto.cruz.tiendauniapi.presentation.dto.UserSummary;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class OrderMapper {

    public static OrderEntity toOrder(OrderRequest request, UserEntity user, PublicationEntity publication, UUID clientOrderKey) {
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

        return OrderEntity.builder()
                .user(user)
                .clientKey(clientOrderKey)
                .paymentMethod(paymentMethod)
                .amountPaid(request.totalAmount())
                .paymentProofUrl(request.paymentProof())
                .status(status)
                .publication(publication)
                .build();
    }

    public static ProductOrderEntity toProductOrder(OrderEntity order, ProductEntity product, ProductOrderItem productOrder) {
        return ProductOrderEntity.builder()
                .order(order)
                .product(product)
                .quantity(productOrder.quantity())
                .unitPrice(productOrder.price())
                .build();
    }

    public static OrderResponse toOrderResponse(OrderEntity order) {
        return new OrderResponse(order.getId(), order.getStatus(), order.getAmountPaid(), order.getPaymentMethod());
    }

    public static ProductOrderDetailResponse toProductOrderDetailResponse(ProductOrderEntity productOrder) {
        ProductEntity product = productOrder.getProduct();

        return new ProductOrderDetailResponse(
                product.getPhotoUrl(),
                product.getName(),
                productOrder.getQuantity(),
                productOrder.getUnitPrice()
        );
    }

    public static List<ProductOrderDetailResponse> toProductOrderDetailResponse(List<ProductOrderEntity> productOrders) {
        return productOrders.stream()
                .map(OrderMapper::toProductOrderDetailResponse)
                .toList();
    }

    public static OrderDetailResponse toOrderDetail(OrderEntity order, List<ProductOrderDetailResponse> productOrdersDetail) {
        return new OrderDetailResponse(
                order.getId(),
                order.getPublication().getId(),
                order.getStatus(),
                order.getAmountPaid(),
                order.getPaymentMethod(),
                productOrdersDetail
        );
    }

    public static ProductOrderSummaryResponse toProductOrderSummary(ProductOrderEntity productOrder) {
        return new ProductOrderSummaryResponse(
                productOrder.getProduct().getPhotoUrl(),
                productOrder.getQuantity());
    }

    public static OrderSummaryResponse toOrderSummary(OrderEntity order) {
        List<ProductOrderSummaryResponse> productOrderSummaries = order.getProductOrders().stream()
                .map(OrderMapper::toProductOrderSummary)
                .toList();

        return new OrderSummaryResponse(
                order.getId(),
                order.getStatus(),
                order.getAmountPaid(),
                order.getPaymentMethod(),
                productOrderSummaries
        );
    }

    public static List<OrderSummaryResponse> toOrderSummary(Collection<OrderEntity> orders) {
        return orders.stream()
                .map(OrderMapper::toOrderSummary)
                .toList();
    }

    public static ProductOrderDetailResponse toProductOrderDetail(ProductOrderEntity productOrder) {
        return new ProductOrderDetailResponse(
                productOrder.getProduct().getPhotoUrl(),
                productOrder.getProduct().getName(),
                productOrder.getQuantity(),
                productOrder.getUnitPrice()
        );
    }

    public static List<ProductOrderDetailResponse> toProductOrderDetail(Collection<ProductOrderEntity> productOrders) {
        return productOrders.stream()
                .map(OrderMapper::toProductOrderDetail)
                .toList();
    }

    public static PostOrderDetail toPostOrderDetail(OrderEntity order) {
        List<ProductOrderDetailResponse> productOrderSummaries = OrderMapper.toProductOrderDetail(order.getProductOrders());

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
    }

    public static List<PostOrderDetail> toPostOrderDetail(Collection<OrderEntity> orders) {
        return orders.stream()
                .map(OrderMapper::toPostOrderDetail)
                .toList();
    }
}
