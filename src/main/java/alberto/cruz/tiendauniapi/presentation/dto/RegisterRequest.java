package alberto.cruz.tiendauniapi.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El correo electrónico no puede estar vacío")
        @Email(message = "El correo electrónico debe tener un formato válido")
        String email,

        @NotBlank(message = "La contraseña no puede estar vacía")
        @Size(min = 8, max = 30, message = "La contraseña debe tener entre 8 y 30 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).*$",
                message = "La contraseña debe contener al menos una mayúscula, una minúscula, un número y un carácter especial"
        )
        String password,

        @NotBlank(message = "El nombre no puede estar vacío")
        @Size(min = 2, max = 60, message = "El nombre debe tener entre 2 y 60 caracteres")
        @Pattern(
                regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]+$",
                message = "El nombre no puede contener números ni caracteres especiales"
        )
        String firstName,

        @NotBlank(message = "El apellido no puede estar vacío")
        @Size(min = 2, max = 60, message = "El apellido debe tener entre 2 y 60 caracteres")
        @Pattern(
                regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]+$",
                message = "El apellido no puede contener números ni caracteres especiales"
        )
        String lastName
) {
}
