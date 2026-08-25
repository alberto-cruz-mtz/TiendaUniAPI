package alberto.cruz.tiendauniapi.service.exception;

public class ExpiredRefreshTokenException extends RuntimeException {
    public ExpiredRefreshTokenException() {
        super("Este token de refresco ha expirado. Por favor, inicia sesión nuevamente para obtener un nuevo token.");
    }
}
