package alberto.cruz.tiendauniapi.utils.mapper;

import alberto.cruz.tiendauniapi.persistence.entity.ProductEntity;
import alberto.cruz.tiendauniapi.presentation.dto.ProductItem;

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
                entity.getPhotoUrl()
        );
    }

    public static List<ProductItem> toProductItem(Collection<ProductEntity> entities) {
        return entities.stream()
                .map(ProductMapper::toProductItem)
                .toList();
    }
}
