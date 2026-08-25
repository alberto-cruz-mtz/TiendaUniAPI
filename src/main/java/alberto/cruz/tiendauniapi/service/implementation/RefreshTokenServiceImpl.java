package alberto.cruz.tiendauniapi.service.implementation;

import alberto.cruz.tiendauniapi.common.ResourceNotFoundException;
import alberto.cruz.tiendauniapi.common.UnknownException;
import alberto.cruz.tiendauniapi.persistence.entity.RefreshTokenEntity;
import alberto.cruz.tiendauniapi.persistence.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl {

    private final RefreshTokenRepository repository;
    private final Long refreshTokenExpirationInSeconds;

    public RefreshTokenServiceImpl(
            RefreshTokenRepository repository,
            @Value("${app.refresh-token.expiration}") Long refreshTokenExpirationInSeconds
    ) {
        this.repository = repository;
        this.refreshTokenExpirationInSeconds = refreshTokenExpirationInSeconds;
    }

    public String generateToken(UUID userId) {
        RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity.builder()
                .token(UUID.randomUUID())
                .userId(userId)
                .expiredAt(Instant.now().plusSeconds(this.refreshTokenExpirationInSeconds))
                .build();

        RefreshTokenEntity savedRefreshToken = repository.save(refreshTokenEntity);

        return savedRefreshToken.getId().toString();
    }

    public String refreshToken(UUID refreshToken, UUID userId) {
        RefreshTokenEntity refreshTokenEntity = repository.findByTokenAndUserId(refreshToken, userId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el token de refresco proporcionado. Por favor, inicia sesión nuevamente para obtener un nuevo token."));

        if (refreshTokenEntity.getExpiredAt().isBefore(Instant.now())) {
            refreshTokenEntity.setRevoked(true);
            repository.save(refreshTokenEntity);

            throw new UnknownException("Este token de refresco ha expirado. Por favor, inicia sesión nuevamente para obtener un nuevo token.");
        }

        if (refreshTokenEntity.isRevoked()) {
            throw new UnknownException("Este token de refresco ha sido revocado. Por favor, inicia sesión nuevamente para obtener un nuevo token.");
        }

        refreshTokenEntity.setToken(UUID.randomUUID());
        refreshTokenEntity.setExpiredAt(Instant.now().plusSeconds(this.refreshTokenExpirationInSeconds));
        RefreshTokenEntity updatedRefreshToken = repository.save(refreshTokenEntity);

        return updatedRefreshToken.getId().toString();
    }

}