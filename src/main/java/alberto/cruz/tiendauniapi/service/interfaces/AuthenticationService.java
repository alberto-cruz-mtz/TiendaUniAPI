package alberto.cruz.tiendauniapi.service.interfaces;

import alberto.cruz.tiendauniapi.presentation.dto.AuthenticationRequest;
import alberto.cruz.tiendauniapi.presentation.dto.AuthenticationResponse;
import alberto.cruz.tiendauniapi.presentation.dto.RegisterRequest;
import alberto.cruz.tiendauniapi.presentation.dto.RegisterResponse;

public interface AuthenticationService {

    RegisterResponse register(RegisterRequest request);

    AuthenticationResponse authenticate(String email, String password);

}
