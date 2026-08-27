package alberto.cruz.tiendauniapi.service.exception;

public class RevokedRefreshTokenException extends RuntimeException {
    public RevokedRefreshTokenException() {
        super("Este token de refresco ha sido revocado. Por favor, inicia sesión nuevamente para obtener un nuevo token.");
    }
}
