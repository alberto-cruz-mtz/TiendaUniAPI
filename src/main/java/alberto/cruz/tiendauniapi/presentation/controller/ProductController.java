package alberto.cruz.tiendauniapi.presentation.controller;

import alberto.cruz.tiendauniapi.persistence.model.AuthenticatedUser;
import alberto.cruz.tiendauniapi.presentation.dto.DataResponse;
import alberto.cruz.tiendauniapi.presentation.dto.ProductItem;
import alberto.cruz.tiendauniapi.presentation.dto.ProductRequest;
import alberto.cruz.tiendauniapi.service.interfaces.ProductService;
import alberto.cruz.tiendauniapi.service.model.ProductId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductItem> createProduct(@RequestBody @Valid ProductRequest request, @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        UUID userId = authenticatedUser.getUserId();
        ProductItem createdProduct = productService.createProduct(request, userId);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("{id}")
                .buildAndExpand(createdProduct.id())
                .toUri();

        return ResponseEntity.created(location).body(createdProduct);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductItem> getProductById(
            @PathVariable("productId") String id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        ProductId productId = new ProductId(id);
        UUID userId = authenticatedUser.getUserId();

        ProductItem product = productService.getProductById(productId, userId);
        return ResponseEntity.ok(product);
    }

    @GetMapping
    public ResponseEntity<DataResponse<ProductItem>> getAllProducts(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        UUID userId = authenticatedUser.getUserId();
        DataResponse<ProductItem> products = productService.getAllProducts(userId);

        return ResponseEntity.ok(products);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Void> updateProduct(
            @PathVariable("productId") String id,
            @RequestBody @Valid ProductRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        ProductId productId = new ProductId(id);
        UUID userId = authenticatedUser.getUserId();

        productService.updateProduct(request, productId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable("productId") String id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        ProductId productId = new ProductId(id);
        UUID userId = authenticatedUser.getUserId();

        productService.deleteProduct(productId, userId);
        return ResponseEntity.noContent().build();
    }
}
