package alberto.cruz.tiendauniapi.service.exception;

public class InsufficientProductStockException extends RuntimeException {
    public InsufficientProductStockException(String productName) {
        super("No hay suficiente stock para el producto: " + productName);
    }
}
