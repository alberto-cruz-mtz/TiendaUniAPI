package alberto.cruz.tiendauniapi.service.model;

import alberto.cruz.tiendauniapi.service.exception.InvalidRefreshTokenException;

import java.util.UUID;

public final class PostId {
    private final UUID value;

    public PostId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El ID de la publicación no puede ser nulo o vacío.");
        }

        if (value.length() < 32) {
            throw new IllegalArgumentException("El ID de la publicación debe tener al menos 32 caracteres.");
        }

        try {
            this.value = UUID.fromString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("El ID de la publicación debe ser un UUID válido.");
        }
    }

    public UUID value() {
        return value;
    }
}
