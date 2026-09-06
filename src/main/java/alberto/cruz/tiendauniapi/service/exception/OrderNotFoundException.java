package alberto.cruz.tiendauniapi.service.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException() {
        super("No fue posible encontrar el pedido solicitada.");
    }
}
