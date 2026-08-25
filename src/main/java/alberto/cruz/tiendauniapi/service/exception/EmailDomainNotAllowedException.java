package alberto.cruz.tiendauniapi.service.exception;

public class EmailDomainNotAllowedException extends RuntimeException {
    public EmailDomainNotAllowedException() {
        super("El dominio del correo electrónico no esta disponible para ninguna universidad. Por favor, use una dirección de correo perteneciente a una universidad registrada.");
    }
}
