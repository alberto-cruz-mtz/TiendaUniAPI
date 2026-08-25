package alberto.cruz.tiendauniapi.service.implementation;

import alberto.cruz.tiendauniapi.persistence.entity.UniversityEntity;
import alberto.cruz.tiendauniapi.persistence.entity.UserEntity;
import alberto.cruz.tiendauniapi.persistence.repository.UniversityRepository;
import alberto.cruz.tiendauniapi.persistence.repository.UserRepository;
import alberto.cruz.tiendauniapi.presentation.dto.AuthenticationResponse;
import alberto.cruz.tiendauniapi.presentation.dto.RegisterRequest;
import alberto.cruz.tiendauniapi.presentation.dto.RegisterResponse;
import alberto.cruz.tiendauniapi.presentation.dto.TokenBundle;
import alberto.cruz.tiendauniapi.service.exception.EmailAddressAlreadyRegistered;
import alberto.cruz.tiendauniapi.service.exception.EmailDomainNotAllowedException;
import alberto.cruz.tiendauniapi.service.exception.EmailAddressNotFound;
import alberto.cruz.tiendauniapi.service.interfaces.AuthenticationService;
import alberto.cruz.tiendauniapi.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
        this.ensureThatEmailAddressIsNotRegistered(request.email());

        UniversityEntity university = this.findUniversityByEmail(request.email());
        String encodedPassword = passwordEncoder.encode(request.password());
        UserEntity user = this.createUser(request, university, encodedPassword);

        UserEntity savedUser = userRepository.save(user);

        Authentication authenticatedToken = this.createAuthentication(savedUser.getEmail());
        TokenBundle tokens = this.generateAccessAndRefreshToken(authenticatedToken, savedUser.getId());

        return this.createRegisterResponse(savedUser, tokens);
    }

    @Override
    public AuthenticationResponse authenticate(String email, String password) {
        Authentication authentication = this.authenticateUserByCredentials(email, password);

        UserEntity user = this.findUserByEmail(email);
        TokenBundle tokens = this.generateAccessAndRefreshToken(authentication, user.getId());

        return this.createAuthenticationResponse(user, tokens);
    }

    private String extractEmailDomain(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex < 0 || atIndex == email.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo no tiene un dominio válido");
        }
        return email.substring(atIndex + 1).toLowerCase();
    }

    private void ensureThatEmailAddressIsNotRegistered(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAddressAlreadyRegistered();
        }
    }

    private UniversityEntity findUniversityByEmail(String email) {
        String domain = extractEmailDomain(email);
        return universityRepository.findByEmailDomainsContains(domain)
                .orElseThrow(EmailDomainNotAllowedException::new);
    }

    private UserEntity createUser(RegisterRequest request, UniversityEntity university, String encodedPassword) {
        return UserEntity.builder()
                .email(request.email())
                .password(encodedPassword)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .university(university)
                .verified(false)
                .build();
    }

    private RegisterResponse createRegisterResponse(UserEntity user, TokenBundle tokenBundle) {
        return new RegisterResponse(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isVerified(),
                tokenBundle
        );
    }

    private TokenBundle generateAccessAndRefreshToken(Authentication authentication, UUID id) {
        String accessToken = jwtUtil.generateToken(authentication);

        // TODO: Implementar la generación de refresh token con persistencia en BD
        String refreshToken = UUID.randomUUID().toString();

        return new TokenBundle(accessToken, refreshToken);
    }

    private Authentication createAuthentication(String email) {
        return this.createAuthentication(email, null);
    }

    private Authentication createAuthentication(String email, String password) {
        return new UsernamePasswordAuthenticationToken(email, password, Collections.emptyList());
    }

    private Authentication authenticateUserByCredentials(String email, String password) {
        var credentials = this.createAuthentication(email, password);

        // internamente, carga el UserDetails,
        // compara la contraseña en texto plano contra el hash de la BD
        // y lanza BadCredentialsException si no coincide.
        return authenticationManager.authenticate(credentials);
    }

    private UserEntity findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EmailAddressNotFound(email));
    }

    private AuthenticationResponse createAuthenticationResponse(UserEntity user, TokenBundle tokens) {
        return new AuthenticationResponse(
                user.getId(),
                null,
                user.getFirstName(),
                user.getLastName(),
                user.isVerified(),
                tokens
        );
    }
}