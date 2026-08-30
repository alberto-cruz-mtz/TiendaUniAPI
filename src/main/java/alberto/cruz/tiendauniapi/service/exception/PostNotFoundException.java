package alberto.cruz.tiendauniapi.service.exception;

public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException() {
        super("No se encontró ninguna publicación con el ID proporcionado");
    }
}
