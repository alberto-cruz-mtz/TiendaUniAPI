package alberto.cruz.tiendauniapi.service.exception;

import alberto.cruz.tiendauniapi.common.ResourceNotFoundException;

public class EmailAddressNotFound extends ResourceNotFoundException {
    public EmailAddressNotFound(String email) {
        super("No se encontró ninguna cuenta con la dirección de correo electrónico: " + email);
    }
}
