package alberto.cruz.tiendauniapi.utils;

import alberto.cruz.tiendauniapi.service.exception.InvalidTokenException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.MissingClaimException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import alberto.cruz.tiendauniapi.persistence.model.AuthenticatedUser;

import java.time.Instant;

@Component
public class JwtUtil {

    private final String issuer;
    private final Long expirationTime;
    private final Algorithm hmac256Algorithm;

    public JwtUtil(
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.secret}") String secretKey,
            @Value("${app.jwt.expiration}") Long expirationTime) {
        this.issuer = issuer;
        this.expirationTime = expirationTime;
        this.hmac256Algorithm = Algorithm.HMAC256(secretKey);
    }

    public String generateToken(AuthenticatedUser authentication) {
        Instant issuedAt = Instant.now();

        return this.createToken(authentication, issuedAt);
    }

    public DecodedJWT validateToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(hmac256Algorithm)
                    .withIssuer(issuer)
                    .build();

            return verifier.verify(token);
        } catch (AlgorithmMismatchException algorithmMismatchException) {
            throw new InvalidTokenException("The algorithm used to sign the token does not match the expected algorithm.");
        } catch (SignatureVerificationException signatureVerificationException) {
            throw new InvalidTokenException("The signature of the token is invalid. The token may have been modified or forged.");
        } catch (TokenExpiredException tokenExpiredException) {
            throw new InvalidTokenException("The token has expired. Please request a new token.");
        } catch (MissingClaimException missingClaimException) {
            throw new InvalidTokenException("The token is missing a required claim. Please ensure that the token contains all necessary claims.");
        } catch (JWTVerificationException exception) {
            throw new InvalidTokenException("Occurred while verifying the token: " + exception.getMessage());
        }
    }

    public String getUserIdFromToken(DecodedJWT token) {
        return token.getClaim("userId").asString();
    }

    public String getTenantIdFromToken(DecodedJWT token) {
        return token.getClaim("tenantId").asString();
    }

    private String createToken(AuthenticatedUser user, Instant issuedAt) {
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(user.getUsername())
                .withIssuedAt(issuedAt)
                .withNotBefore(issuedAt)
                .withExpiresAt(issuedAt.plusSeconds(expirationTime))
                .withClaim("userId", user.getUserId().toString())
                .withClaim("tenantId", user.getUniversityId().toString())
                .sign(hmac256Algorithm);
    }
}