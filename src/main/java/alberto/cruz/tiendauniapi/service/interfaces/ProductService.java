package alberto.cruz.tiendauniapi.service.interfaces;

import alberto.cruz.tiendauniapi.presentation.dto.DataResponse;
import alberto.cruz.tiendauniapi.presentation.dto.ProductItem;
import alberto.cruz.tiendauniapi.presentation.dto.ProductRequest;
import alberto.cruz.tiendauniapi.service.model.ProductId;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    ProductItem createProduct(ProductRequest request, UUID userId);

    ProductItem getProductById(ProductId id, UUID userId);

    DataResponse<ProductItem> getAllProducts(UUID userId);

    void updateProduct(ProductRequest request, ProductId id, UUID userId);

    void deleteProduct(ProductId id, UUID userId);

}
