package alberto.cruz.tiendauniapi.service.exception;

public class EmailAddressAlreadyRegisteredException extends RuntimeException {
    public EmailAddressAlreadyRegisteredException() {
        super("Este correo ya está registrado. Por favor, use una dirección de correo diferente.");
    }
}
