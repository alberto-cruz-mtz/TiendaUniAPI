package alberto.cruz.tiendauniapi.persistence.entity;

import alberto.cruz.tiendauniapi.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class ProductEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 70)
    private String name;

    @Builder.Default
    @Column(name = "quantity", nullable = false, columnDefinition = "NUMERIC(10,2)")
    private BigDecimal quantity = new BigDecimal("0.00");

    @Builder.Default
    @Column(name = "sale_price", nullable = false, columnDefinition = "NUMERIC(10,2)")
    private BigDecimal salePrice = new BigDecimal("0.00");

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false, length = 30)
    private SaleType saleType;

    @Column(name = "photo_url", nullable = false, length = 300)
    private String photoUrl;
}
