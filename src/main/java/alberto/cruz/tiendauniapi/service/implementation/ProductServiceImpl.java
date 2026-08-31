package alberto.cruz.tiendauniapi.service.implementation;

import alberto.cruz.tiendauniapi.persistence.entity.CategoryEntity;
import alberto.cruz.tiendauniapi.persistence.entity.CategoryName;
import alberto.cruz.tiendauniapi.persistence.entity.ProductEntity;
import alberto.cruz.tiendauniapi.persistence.entity.SaleType;
import alberto.cruz.tiendauniapi.persistence.entity.UserEntity;
import alberto.cruz.tiendauniapi.persistence.repository.CategoryRepository;
import alberto.cruz.tiendauniapi.persistence.repository.ProductRepository;
import alberto.cruz.tiendauniapi.presentation.dto.DataResponse;
import alberto.cruz.tiendauniapi.presentation.dto.ProductItem;
import alberto.cruz.tiendauniapi.presentation.dto.ProductRequest;
import alberto.cruz.tiendauniapi.service.exception.CategoryNotFoundException;
import alberto.cruz.tiendauniapi.service.exception.ProductNotFoundException;
import alberto.cruz.tiendauniapi.service.interfaces.ProductService;
import alberto.cruz.tiendauniapi.service.interfaces.UserService;
import alberto.cruz.tiendauniapi.service.model.ProductId;
import alberto.cruz.tiendauniapi.utils.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;

    @Override
    @Transactional
    public ProductItem createProduct(ProductRequest request, UUID userId) {
        UserEntity user = userService.getUserById(userId);
        CategoryEntity category = this.findCategoryByName(request.category());

        ProductEntity product = ProductMapper.toEntity(request, category, user);
        ProductEntity savedProduct = productRepository.save(product);

        return ProductMapper.toProductItem(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductItem getProductById(ProductId id, UUID userId) {
        ProductEntity product = this.findProductByIdAndUserId(id, userId);
        return ProductMapper.toProductItem(product);
    }

    @Override
    @Transactional(readOnly = true)
    public DataResponse<ProductItem> getAllProducts(UUID userId) {
        List<ProductEntity> products = productRepository.findAllByUserId(userId);
        List<ProductItem> productItems = ProductMapper.toProductItem(products);

        return new DataResponse<>(productItems);
    }

    @Override
    @Transactional
    public void updateProduct(ProductRequest request, ProductId id, UUID userId) {
        ProductEntity product = this.findProductByIdAndUserId(id, userId);

        CategoryEntity category = this.findCategoryByName(request.category());
        this.updateProductFields(product, request, category);

        productRepository.save(product);
    }

    @Override
    @Transactional
    public void deleteProduct(ProductId id, UUID userId) {
        ProductEntity product = this.findProductByIdAndUserId(id, userId);
        productRepository.delete(product);
    }

    private CategoryEntity findCategoryByName(String name) {
        CategoryName categoryName = CategoryName.valueOf(name.toUpperCase());

        return categoryRepository.findByName(categoryName)
                .orElseThrow(CategoryNotFoundException::new);
    }

    private ProductEntity findProductByIdAndUserId(ProductId id, UUID userId) {
        return productRepository.findByIdAndUserId(id.value(), userId)
                .orElseThrow(ProductNotFoundException::new);
    }

    private void updateProductFields(ProductEntity product, ProductRequest request, CategoryEntity category) {
        SaleType saleType = SaleType.valueOf(request.saleType().toUpperCase());

        product.setPhotoUrl(request.photoUrl());
        product.setName(request.name());
        product.setQuantity(request.quantity());
        product.setSalePrice(request.salePrice());
        product.setSaleType(saleType);
        product.setCategory(category);
    }
}
