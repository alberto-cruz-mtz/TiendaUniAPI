package alberto.cruz.tiendauniapi.service.exception;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException() {
        super("No se encontró la categoría especificada.");
    }
}
