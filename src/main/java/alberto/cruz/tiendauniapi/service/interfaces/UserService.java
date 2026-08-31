package alberto.cruz.tiendauniapi.service.interfaces;

import alberto.cruz.tiendauniapi.persistence.entity.UserEntity;
import alberto.cruz.tiendauniapi.persistence.projection.UserProjection;

import java.util.UUID;

public interface UserService {

    UserEntity getUserById(UUID userId);

    UserEntity getUserByEmail(String email);

    UserProjection getUserProjectionById(UUID userId);

    UserProjection getUserProjectionByEmail(String email);

    boolean existsUserByEmail(String email);

    UserEntity saveUser(UserEntity user);
}
