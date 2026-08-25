package alberto.cruz.tiendauniapi.service.interfaces;

import alberto.cruz.tiendauniapi.presentation.dto.AuthenticationRequest;
import alberto.cruz.tiendauniapi.presentation.dto.AuthenticationResponse;
import alberto.cruz.tiendauniapi.presentation.dto.RegisterRequest;
import alberto.cruz.tiendauniapi.presentation.dto.RegisterResponse;
import alberto.cruz.tiendauniapi.presentation.dto.TokenBundle;

import java.util.UUID;

public interface AuthenticationService {

    RegisterResponse register(RegisterRequest request);

    AuthenticationResponse authenticate(String email, String password);

    TokenBundle refreshTokenAndGenerateAccessToken(UUID refreshToken, UUID userId);

    void logout(UUID refreshToken, UUID userId);
}
