package alberto.cruz.tiendauniapi.service.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("No fue posible encontrar el usuario.");
    }
}
