package alberto.cruz.tiendauniapi.service.exception;

public class EmailAddressAlreadyRegistered extends RuntimeException {
    public EmailAddressAlreadyRegistered() {
        super("Este correo ya está registrado. Por favor, use una dirección de correo diferente.");
    }
}
