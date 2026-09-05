package alberto.cruz.tiendauniapi.service.model;

import alberto.cruz.tiendauniapi.service.exception.InvalidArgumentException;

import java.util.UUID;

public final class OrderId {

    private final UUID value;

    public OrderId(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidArgumentException("orderId", "El ID de la order no puede ser nulo o vacío.");
        }

        if (value.length() < 36) {
            throw new InvalidArgumentException("orderId", "El ID de la orden debe tener al menos 36 caracteres.");
        }

        try {
            this.value = UUID.fromString(value);
        } catch (Exception e) {
            throw new InvalidArgumentException("orderId", "El ID de la orden debe ser un UUID válido.");
        }
    }

    public UUID value() {
        return value;
    }
}
