package alberto.cruz.tiendauniapi.utils.mapper;

import alberto.cruz.tiendauniapi.persistence.entity.CategoryEntity;
import alberto.cruz.tiendauniapi.persistence.entity.ProductEntity;
import alberto.cruz.tiendauniapi.persistence.entity.SaleType;
import alberto.cruz.tiendauniapi.persistence.entity.UserEntity;
import alberto.cruz.tiendauniapi.presentation.dto.ProductItem;
import alberto.cruz.tiendauniapi.presentation.dto.ProductRequest;

import java.util.Collection;
import java.util.List;

public class ProductMapper {

    public static ProductItem toProductItem(ProductEntity entity) {
        return new ProductItem(
                entity.getId(),
                entity.getName(),
                entity.getQuantity(),
                entity.getSalePrice(),
                entity.getCategory().getName().name(),
                entity.getSaleType(),
                entity.getPhotoUrl()
        );
    }

    public static List<ProductItem> toProductItem(Collection<ProductEntity> entities) {
        return entities.stream()
                .map(ProductMapper::toProductItem)
                .toList();
    }

    public static ProductEntity toEntity(ProductRequest request, CategoryEntity category, UserEntity user) {
        SaleType saleType = SaleType.valueOf(request.saleType().toUpperCase());

        return ProductEntity.builder()
                .name(request.name())
                .quantity(request.quantity())
                .salePrice(request.salePrice())
                .category(category)
                .photoUrl(request.photoUrl())
                .saleType(saleType)
                .user(user)
                .build();
    }
}
