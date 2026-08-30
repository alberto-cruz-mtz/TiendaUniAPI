package alberto.cruz.tiendauniapi.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_order")
public class ProductOrderEntity {

    @EmbeddedId
    private ProductOrderId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("productId")
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("orderId")
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "quantity", nullable = false, columnDefinition = "NUMERIC(10,2)")
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, columnDefinition = "NUMERIC(10,2)")
    private BigDecimal unitPrice;
}
