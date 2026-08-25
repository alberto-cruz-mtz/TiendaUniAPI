package alberto.cruz.tiendauniapi.presentation.controller;

import alberto.cruz.tiendauniapi.persistence.model.AuthenticatedUser;
import alberto.cruz.tiendauniapi.persistence.model.RefreshToken;
import alberto.cruz.tiendauniapi.presentation.dto.AuthenticationRequest;
import alberto.cruz.tiendauniapi.presentation.dto.AuthenticationResponse;
import alberto.cruz.tiendauniapi.presentation.dto.RefreshTokenRequest;
import alberto.cruz.tiendauniapi.presentation.dto.RegisterRequest;
import alberto.cruz.tiendauniapi.presentation.dto.RegisterResponse;
import alberto.cruz.tiendauniapi.service.interfaces.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final Duration refreshTokenExpirationInSeconds;
    private final Duration accessTokenExpirationInSeconds;

    public AuthenticationController(
            AuthenticationService authenticationService,
            @Value("${app.refresh-token.expiration}") Long refreshTokenExpirationInSeconds,
            @Value("${app.jwt.secret}") Long accessTokenExpirationInSeconds
    ) {
        this.authenticationService = authenticationService;
        this.refreshTokenExpirationInSeconds = Duration.ofSeconds(refreshTokenExpirationInSeconds);
        this.accessTokenExpirationInSeconds = Duration.ofSeconds(accessTokenExpirationInSeconds);
    }

    @PostMapping("/signup")
    public ResponseEntity<RegisterResponse> registerNewUser(@RequestBody @Valid RegisterRequest request) {
        var response = authenticationService.register(request);
        var tokens = response.tokenBundle();

        String accessTokenCookie = this.createAccessTokenCookie(tokens.accessToken());
        String refreshTokenCookie = this.createRefreshTokenCookie(tokens.refreshToken());

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie, refreshTokenCookie)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticateUser(@RequestBody @Valid AuthenticationRequest request) {
        var response = authenticationService.authenticate(request.email(), request.password());
        var tokens = response.tokenBundle();

        String accessTokenCookie = this.createAccessTokenCookie(tokens.accessToken());
        String refreshTokenCookie = this.createRefreshTokenCookie(tokens.refreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie, refreshTokenCookie)
                .body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshToken(@CookieValue("refresh-token") String refreshToken, @RequestBody @Valid RefreshTokenRequest request) {
        RefreshToken token = new RefreshToken(refreshToken);
        var tokens = authenticationService.refreshTokenAndGenerateAccessToken(token.value(), request.userId());

        String accessTokenCookie = this.createAccessTokenCookie(tokens.accessToken());
        String refreshTokenCookie = this.createRefreshTokenCookie(tokens.refreshToken());

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie, refreshTokenCookie)
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logoutUser(@CookieValue("refresh-token") String refreshToken, @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        RefreshToken token = new RefreshToken(refreshToken);
        authenticationService.logout(token.value(), authenticatedUser.getUserId());

        String accessTokenCookie = this.createAccessTokenCookie(null);
        String refreshTokenCookie = this.createRefreshTokenCookie(null);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie, refreshTokenCookie)
                .build();
    }

    private String createAccessTokenCookie(String token) {
        String value = token != null ? token : "";
        return this.createCookie("access-token", value, "/", this.accessTokenExpirationInSeconds);
    }

    private String createRefreshTokenCookie(String token) {
        String value = token != null ? token : "";
        return this.createCookie("refresh-token", value, "/auth", this.refreshTokenExpirationInSeconds);
    }

    private String createCookie(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build()
                .toString();

    }
}