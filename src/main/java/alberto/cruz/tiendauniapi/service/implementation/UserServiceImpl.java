package alberto.cruz.tiendauniapi.service.implementation;

import alberto.cruz.tiendauniapi.persistence.entity.UserEntity;
import alberto.cruz.tiendauniapi.persistence.projection.UserProjection;
import alberto.cruz.tiendauniapi.persistence.repository.UserRepository;
import alberto.cruz.tiendauniapi.service.exception.EmailAddressNotFound;
import alberto.cruz.tiendauniapi.service.exception.UserNotFoundException;
import alberto.cruz.tiendauniapi.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserEntity getUserById(UUID userId) {
        Objects.requireNonNull(userId, "User ID must not be null");

        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    @Override
    public UserEntity getUserByEmail(String email) {
        Objects.requireNonNull(email, "Email must not be null");

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EmailAddressNotFound(email));
    }

    @Override
    public UserProjection getUserProjectionById(UUID userId) {
        Objects.requireNonNull(userId, "User ID must not be null");

        return userRepository.findUserEntitiesById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    @Override
    public UserProjection getUserProjectionByEmail(String email) {
        Objects.requireNonNull(email, "Email must not be null");

        return userRepository.findUserEntitiesByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No se encontró ninguna cuenta con la dirección de correo electrónico: " + email));
    }

    @Override
    public boolean existsUserByEmail(String email) {
        Objects.requireNonNull(email, "Email must not be null");

        return userRepository.existsByEmail(email);
    }

    @Override
    public UserEntity saveUser(UserEntity user) {
        Objects.requireNonNull(user, "User entity must not be null");

        return userRepository.save(user);
    }

}
