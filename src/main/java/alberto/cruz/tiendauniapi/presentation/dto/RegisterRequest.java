package alberto.cruz.tiendauniapi.presentation.dto;

public record RegisterRequest(
        String email,
        String password,
        String firstName,
        String lastName
) {
}
