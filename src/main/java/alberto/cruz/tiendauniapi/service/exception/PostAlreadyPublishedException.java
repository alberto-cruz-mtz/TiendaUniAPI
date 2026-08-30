package alberto.cruz.tiendauniapi.service.exception;

public class PostAlreadyPublishedException extends RuntimeException {
    public PostAlreadyPublishedException() {
        super("La publicación ya esta publicada en este momento.");
    }
}
