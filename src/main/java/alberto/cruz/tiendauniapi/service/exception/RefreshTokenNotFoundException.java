package alberto.cruz.tiendauniapi.service.exception;

import alberto.cruz.tiendauniapi.common.ResourceNotFoundException;

public class RefreshTokenNotFoundException extends ResourceNotFoundException {
    public RefreshTokenNotFoundException() {
        super("No se encontró el token de refresco proporcionado. Por favor, inicia sesión nuevamente para obtener un nuevo token.");
    }
}
