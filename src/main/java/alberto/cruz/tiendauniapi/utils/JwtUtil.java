package alberto.cruz.tiendauniapi.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {

    private final String issuer;
    private final String secretKey;
    private final Long expirationTime;

    public JwtUtil(
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.secret}") String secretKey,
            @Value("${app.jwt.expiration}") Long expirationTime) {
        this.issuer = issuer;
        this.secretKey = secretKey;
        this.expirationTime = expirationTime;
    }

    public String generateToken(Authentication authentication) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        Instant issuedAt = Instant.now();

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(authentication.getName())
                .withIssuedAt(Date.from(issuedAt))
                .withNotBefore(Date.from(issuedAt))
                .withExpiresAt(Date.from(issuedAt.plusSeconds(expirationTime)))
                .sign(algorithm);
    }

    public DecodedJWT validateToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build();
        return verifier.verify(token);
    }
}
