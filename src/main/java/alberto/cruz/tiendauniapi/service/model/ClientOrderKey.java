package alberto.cruz.tiendauniapi.service.model;

import alberto.cruz.tiendauniapi.service.exception.InvalidArgumentException;

import java.util.UUID;

public final class ClientOrderKey {

    private final UUID value;

    public ClientOrderKey(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidArgumentException(
                    "clientOrderKey",
                    "La clave de idempotencia es requerida y no puede estar vacía. Envía el header 'Idempotency-Key' con un UUID válido."
            );
        }

        if (value.length() < 32) {
            throw new InvalidArgumentException(
                    "clientOrderKey",
                    "La clave de idempotencia debe tener al menos 32 caracteres en formato UUID. Se recibió un valor con longitud insuficiente: '" + value + "'."
            );
        }

        try {
            this.value = UUID.fromString(value);
        } catch (Exception e) {
            throw new InvalidArgumentException(
                    "clientOrderKey",
                    "La clave de idempotencia no tiene un formato UUID válido. Asegúrate de enviar un UUID estándar con el patrón 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'. Valor recibido: '" + value + "'."
            );
        }
    }

    public UUID value() {
        return value;
    }
}
