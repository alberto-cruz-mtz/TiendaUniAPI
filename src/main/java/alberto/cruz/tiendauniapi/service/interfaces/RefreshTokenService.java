package alberto.cruz.tiendauniapi.service.interfaces;

import java.util.UUID;

public interface RefreshTokenService {

    String generateToken(UUID userId);

    String refreshToken(UUID refreshToken, UUID userId);

    void revokeToken(UUID refreshToken, UUID userId);
}
