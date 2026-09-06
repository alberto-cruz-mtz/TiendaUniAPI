package alberto.cruz.tiendauniapi.service.exception;

public class ProductPriceChangedException extends RuntimeException {
    public ProductPriceChangedException(String productName) {
        super("El precio del producto ha cambiado: " + productName);
    }
}
