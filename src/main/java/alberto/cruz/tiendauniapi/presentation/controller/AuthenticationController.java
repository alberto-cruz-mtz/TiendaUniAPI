package alberto.cruz.tiendauniapi.presentation.controller;

import alberto.cruz.tiendauniapi.presentation.dto.AuthenticationRequest;
import alberto.cruz.tiendauniapi.presentation.dto.AuthenticationResponse;
import alberto.cruz.tiendauniapi.presentation.dto.RegisterRequest;
import alberto.cruz.tiendauniapi.presentation.dto.RegisterResponse;
import alberto.cruz.tiendauniapi.service.interfaces.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/signup")
    public ResponseEntity<RegisterResponse> registerNewUser(@RequestBody @Valid RegisterRequest request) {
        var response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticateUser(@RequestBody @Valid AuthenticationRequest request) {
        var response = authenticationService.authenticate(request.email(), request.password());
        return ResponseEntity.ok(response);
    }
}
