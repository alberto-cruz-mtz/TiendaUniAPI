package alberto.cruz.tiendauniapi.persistence.model;

import alberto.cruz.tiendauniapi.service.exception.InvalidRefreshTokenException;

import java.util.UUID;

public final class RefreshToken {
    private final UUID value;

    public RefreshToken(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRefreshTokenException("El token de actualización no puede ser nulo o vacío.");
        }

        if (value.length() <= 32) {
            throw new InvalidRefreshTokenException("El token de actualización debe tener al menos 32 caracteres.");
        }

        try {
            this.value = UUID.fromString(value);
        } catch (Exception e) {
            throw new InvalidRefreshTokenException("El token de actualización debe ser un UUID válido.");
        }
    }

    public UUID value() {
        return value;
    }
}
