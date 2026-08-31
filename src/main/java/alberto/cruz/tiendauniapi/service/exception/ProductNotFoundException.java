package alberto.cruz.tiendauniapi.service.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException() {
        super("No fue posible encontrar el producto especificado.");
    }
}
