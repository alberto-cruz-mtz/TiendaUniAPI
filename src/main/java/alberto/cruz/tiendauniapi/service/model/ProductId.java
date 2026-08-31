package alberto.cruz.tiendauniapi.service.model;

import alberto.cruz.tiendauniapi.service.exception.InvalidArgumentException;

import java.util.UUID;

public final class ProductId {

    private final UUID value;

    public ProductId(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidArgumentException("productId", "El ID del producto no puede ser nulo o vacío.");
        }

        if (value.length() < 32) {
            throw new InvalidArgumentException("productId", "El ID del producto debe tener al menos 32 caracteres.");
        }

        try {
            this.value = UUID.fromString(value);
        } catch (Exception e) {
            throw new InvalidArgumentException("productId", "El ID del producto debe ser un UUID válido.");
        }
    }

    public UUID value() {
        return value;
    }
}
