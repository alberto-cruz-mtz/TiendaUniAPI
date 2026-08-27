package alberto.cruz.tiendauniapi.utils.mapper;

import alberto.cruz.tiendauniapi.persistence.entity.UserEntity;
import alberto.cruz.tiendauniapi.persistence.model.AuthenticatedUser;
import alberto.cruz.tiendauniapi.persistence.projection.UserProjection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;

public class UserMapper {

    public static AuthenticatedUser toAuthenticatedUser(UserEntity user) {
        return new AuthenticatedUser(user.getEmail(), user.getPassword(), user.getId(), user.getUniversity().getId());
    }

    public static AuthenticatedUser toAuthenticatedUser(UserProjection user) {
        return new AuthenticatedUser(user.getEmail(), user.getPassword(), user.getId(), user.getUniversityId());
    }

    public static Authentication toAuthentication(AuthenticatedUser authenticatedUser) {
        return new UsernamePasswordAuthenticationToken(authenticatedUser, null, Collections.emptyList());
    }
}
