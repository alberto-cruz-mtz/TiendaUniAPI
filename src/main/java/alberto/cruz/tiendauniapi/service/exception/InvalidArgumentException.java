package alberto.cruz.tiendauniapi.service.exception;

import lombok.Getter;

@Getter
public class InvalidArgumentException extends RuntimeException {
    private final String argumentName;

    public InvalidArgumentException(String argumentName, String message) {
        super(message);
        this.argumentName = argumentName;
    }

}
