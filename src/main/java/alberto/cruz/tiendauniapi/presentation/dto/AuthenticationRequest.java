package alberto.cruz.tiendauniapi.presentation.dto;

public record AuthenticationRequest(
        String email,
        String password
) {
}
