package alberto.cruz.tiendauniapi.service.implementation;

import alberto.cruz.tiendauniapi.common.ResourceNotFoundException;
import alberto.cruz.tiendauniapi.common.UnknownException;
import alberto.cruz.tiendauniapi.persistence.entity.RefreshTokenEntity;
import alberto.cruz.tiendauniapi.persistence.repository.RefreshTokenRepository;
import alberto.cruz.tiendauniapi.service.interfaces.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final Long refreshTokenExpirationInSeconds;

    public RefreshTokenServiceImpl(
            RefreshTokenRepository repository,
            @Value("${app.refresh-token.expiration}") Long refreshTokenExpirationInSeconds
    ) {
        this.repository = repository;
        this.refreshTokenExpirationInSeconds = refreshTokenExpirationInSeconds;
    }

    @Override
    @Transactional
    public String generateToken(UUID userId) {
        RefreshTokenEntity refreshTokenEntity = this.createRefreshToken(userId);
        RefreshTokenEntity savedRefreshToken = repository.save(refreshTokenEntity);
        return savedRefreshToken.getId().toString();
    }

    @Override
    @Transactional
    public String refreshToken(UUID refreshToken, UUID userId) {
        RefreshTokenEntity refreshTokenEntity = this.findRefreshTokenByCurrentTokenAndUserId(refreshToken, userId);

        this.ensureThatRefreshTokenIsNotExpired(refreshTokenEntity);
        this.revokeAllActiveTokensByUserId(refreshTokenEntity.isRevoked(), userId);

        RefreshTokenEntity updatedRefreshToken = this.changeCurrentRefreshTokenForNewOne(refreshTokenEntity);

        return updatedRefreshToken.getId().toString();
    }

    private RefreshTokenEntity findRefreshTokenByCurrentTokenAndUserId(UUID refreshToken, UUID userId) {
        return repository.findByTokenAndUserId(refreshToken, userId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el token de refresco proporcionado. Por favor, inicia sesión nuevamente para obtener un nuevo token."));
    }

    private RefreshTokenEntity changeCurrentRefreshTokenForNewOne(RefreshTokenEntity refreshToken) {
        refreshToken.setToken(UUID.randomUUID());
        refreshToken.setExpiredAt(this.calculateExpirationTime());
        return repository.save(refreshToken);
    }

    private void ensureThatRefreshTokenIsNotExpired(RefreshTokenEntity refreshToken) {
        if (refreshToken.getExpiredAt().isBefore(Instant.now())) {
            refreshToken.setRevoked(true);
            repository.save(refreshToken);

            throw new UnknownException("Este token de refresco ha expirado. Por favor, inicia sesión nuevamente para obtener un nuevo token.");
        }
    }

    private void revokeAllActiveTokensByUserId(boolean isRevoked, UUID userId) {
        if (isRevoked) {
            repository.markedLikeRevokedAllTokensByUserId(userId);
            throw new UnknownException("Este token de refresco ha sido revocado. Por favor, inicia sesión nuevamente para obtener un nuevo token.");
        }
    }

    private Instant calculateExpirationTime() {
        return Instant.now().plusSeconds(this.refreshTokenExpirationInSeconds);
    }

    private RefreshTokenEntity createRefreshToken(UUID userId) {
        return RefreshTokenEntity.builder()
                .token(UUID.randomUUID())
                .userId(userId)
                .expiredAt(this.calculateExpirationTime())
                .build();
    }

}