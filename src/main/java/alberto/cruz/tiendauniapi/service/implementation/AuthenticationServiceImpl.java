package alberto.cruz.tiendauniapi.service.implementation;

import alberto.cruz.tiendauniapi.persistence.entity.UniversityEntity;
import alberto.cruz.tiendauniapi.persistence.entity.UserEntity;
import alberto.cruz.tiendauniapi.persistence.repository.UniversityRepository;
import alberto.cruz.tiendauniapi.persistence.repository.UserRepository;
import alberto.cruz.tiendauniapi.presentation.dto.AuthenticationResponse;
import alberto.cruz.tiendauniapi.presentation.dto.RegisterRequest;
import alberto.cruz.tiendauniapi.presentation.dto.RegisterResponse;
import alberto.cruz.tiendauniapi.service.interfaces.AuthenticationService;
import alberto.cruz.tiendauniapi.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Override
    public RegisterResponse register(RegisterRequest request) {
        // 1. Validar que el correo no esté registrado
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }

        // 2. Extraer el dominio y validar que pertenezca a una universidad registrada
        String domain = extractEmailDomain(request.email());
        UniversityEntity university = universityRepository.findByEmailDomainsContains(domain)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El dominio del correo no pertenece a una universidad registrada"));

        // 3. Codificar la contraseña
        String encodedPassword = passwordEncoder.encode(request.password());

        // 4. Mapear los datos a UserEntity (sin mapper)
        UserEntity user = new UserEntity();
        user.setEmail(request.email());
        user.setPassword(encodedPassword);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setUniversityId(university);

        // 5. Guardar el usuario en la BD
        UserEntity savedUser = userRepository.save(user);

        // 6. Generar el token JWT
        var authentication = new UsernamePasswordAuthenticationToken(
                savedUser.getEmail(),
                null,
                Collections.emptyList());
        String accessToken = jwtUtil.generateToken(authentication);

        // 7. Generar un refreshToken (fallback hasta que llegue la implementación definitiva)
        String refreshToken = UUID.randomUUID().toString();

        // 8. Retornar la respuesta
        return new RegisterResponse(
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                false,
                accessToken,
                refreshToken);
    }

    @Override
    public AuthenticationResponse authenticate(String email, String password) {
        return null;
    }

    private String extractEmailDomain(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex < 0 || atIndex == email.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo no tiene un dominio válido");
        }
        return email.substring(atIndex + 1).toLowerCase();
    }
}
